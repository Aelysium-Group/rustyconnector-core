package group.aelysium.rustyconnector.plugin.velocity.lib.dynamic_scale;

import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.ArrayList;
import java.util.List;

public class DynamicScaler {
    private static final K8Service k8 = new K8Service();
    protected final Settings settings;
    protected final IFamily family;

    public DynamicScaler(Settings settings, IFamily family) {
        this.settings = settings;
        this.family = family;
    }

    /**
     * Creates `n` number of new MCLoaders.
     */
    public void createMCLoaders(int count) {
        for (int i = 0; i < count; i++)
            k8.createPod(this.family.id(), this.settings.helmChart());
    }

    /**
     * Deletes the MCLoader from the Kubernetes cluster.
     * If the MCLoader is not contained within a pod (specifically if it is not contained within a pod with the env variable "POD_NAME" set)
     * this method won't do anything.
     */
    public void deleteMCLoader(IMCLoader mcLoader) {
        if(mcLoader.podName().isEmpty()) return;
        k8.deletePod(this.family.id(), mcLoader.podName().get());
    }

    /**
     * Returns a list of all the Pods that are being managed by the Dynamic Scaler.
     */
    public List<Pod> pods() {
        return k8.familyPods(this.family.id());
    }

    /**
     * Returns a list of the Pods which have fully graduated and are now MCLoaders.
     * In more technical terms, these pods have finished booting up and their copy of RustyConnector has successfully
     * registered to the proxy.
     */
    public List<IMCLoader> graduatedPods() {
        List<Pod> pods = k8.familyPods(this.family.id());
        List<IMCLoader> mcLoaders = this.family.loadBalancer().servers();

        List<IMCLoader> output = new ArrayList<>();

        if(pods.size() < mcLoaders.size())
            pods.forEach(p -> output.addAll(mcLoaders.stream().filter(m -> {
                if(m.podName().isEmpty()) return false;
                return m.podName().get().equals(p.getMetadata().getName());
            }).toList()));
        else
            mcLoaders.forEach(m -> {
                if(m.podName().isEmpty()) return;
                if(pods.stream().noneMatch(p -> p.getMetadata().getName().equals(m.podName().get()))) return;

                output.add(m);
            });

        return output;
    }

    /**
     * Returns a list of Pods which haven't graduated to becoming MCLoaders.
     * In more technical terms, these pods have not finished booting up and the copy of RustyConnector
     * running on them has not yet booted/registered to the proxy.
     */
    public List<Pod> undergraduatePods() {
        List<Pod> pods = k8.familyPods(this.family.id());
        List<IMCLoader> mcLoaders = this.family.loadBalancer().servers();

        List<Pod> output = new ArrayList<>(pods);

        pods.forEach(p -> {
            if(mcLoaders.stream().anyMatch(m -> m.podName().orElse("").equals(p.getMetadata().getName()))) return;
            output.add(p);
        });

        return output;
    }

    public void collectTelemetry() {
        long count = this.family.playerCount();
    }

    public record Settings(
            boolean enabled,
            String helmChart,
            int maxPods,
            Reactive reactive,
            ProactiveAlgorithm proactiveAlgorithm,
            Scheduled scheduled,
            Predictive predictive
            ) {
        public record Reactive(
                Generation generation,
                Degeneration degeneration
        ) {
            public record Generation(
                    int ratio,
                    LiquidTimestamp delay,
                    int count
            ){}
            public record Degeneration(
                    int ratio,
                    LiquidTimestamp delay
            ) {}
        }

        public enum ProactiveAlgorithm {
            NONE,
            SCHEDULED,
            PREDICTIVE
        }

        public record Scheduled(
            String start,
            Object timespans
        ) {}

        public record Predictive(
            String keepTelemetryFor,
            String resolution,
            double idealRatio
        ) {}
    }
}
