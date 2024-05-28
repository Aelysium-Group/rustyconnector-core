package group.aelysium.rustyconnector.toolkit.common.config;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public abstract class IConfigService {
    /**
     * Gets the version that this config service is using.
     */
    public abstract int version();

    public abstract void put(IYAML config);

    public abstract <TConfig extends IYAML> Optional<TConfig> get(ConfigKey key);

    public abstract void remove(ConfigKey key);

    public record ConfigKey(@NotNull Class<? extends IYAML> type, String name) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigKey configKey = (ConfigKey) o;
            return Objects.equals(type, configKey.type) && Objects.equals(name, configKey.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name);
        }

        public static ConfigKey singleton(Class<? extends IYAML> type) {
            return new ConfigKey(type, null);
        }
    }
}