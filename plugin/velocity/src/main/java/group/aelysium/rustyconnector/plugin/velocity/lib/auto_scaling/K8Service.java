package group.aelysium.rustyconnector.plugin.velocity.lib.auto_scaling;

import group.aelysium.rustyconnector.core.lib.crypt.Token;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.Family;
import group.aelysium.rustyconnector.toolkit.core.serviceable.interfaces.Service;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.*;

import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.NoSuchElementException;

public class K8Service implements Service {
    protected Token token = new Token(16, new SecureRandom(), Token.lower);

    public K8Service() {}

    public List<Pod> familyPods(String familyName) {
        try {
            new Family.Reference(familyName).get();
        } catch (Exception ignore) {
            throw new NoSuchElementException("A family with the id "+familyName+" doesn't exist!");
        }

        String namespace = getNamespaceName(familyName);

        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            return client.pods().inNamespace(namespace).list().getItems();
        }
    }

    public void createServer(String familyName, String containerName, String containerImage) {
        String podName = newPodName(familyName);
        String namespace = getNamespaceName(familyName);

        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            /*
        }
            List<HasMetadata> result = client.load(new FileInputStream("/")).get();
            for (HasMetadata resource : result) {
                if (!(resource instanceof Deployment deployment)) continue;

                deployment.getMetadata().setName(podName);
                deployment.getMetadata().setNamespace(namespace);
            }
            client.resourceList(result).create();*/

            Pod pod = new PodBuilder()
                    .withNewMetadata()
                        .withName(podName)
                    .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName(containerName)
                            .withImage(containerImage)
                        .endContainer()
                    .endSpec()
                    .build();
            client.pods().inNamespace(namespace).resource(pod).create();
        }
    }

    public void deleteServer(String familyName, String podName) {
        String namespace = getNamespaceName(familyName);

        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            client.pods().inNamespace(namespace).withName(podName.toLowerCase()).delete();
        }
    }

    private String getNamespaceName(String familyName) {
        return ("rcf-"+familyName).toLowerCase();
    }

    private String newPodName(String familyName) {
        return (familyName+"-"+token.nextString()).toLowerCase();
    }

    @Override
    public void kill() {

    }
}
