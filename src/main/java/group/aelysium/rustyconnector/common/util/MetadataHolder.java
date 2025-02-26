package group.aelysium.rustyconnector.common.util;

import java.util.Map;
import java.util.Optional;

public interface MetadataHolder<T> {
    /**
     * Stores metadata in the family.
     * @param key The name of the metadata to store.
     * @param value The metadata to store.
     * @return `true` if the metadata could be stored. `false` if the name of the metadata is already in use.
     */
    boolean storeMetadata(String key, T value);
    
    /**
     * Fetches metadata from the family.
     * @param key The name of the metadata to fetch.
     * @return An optional containing the metadata, or an empty metadata if no metadata could be found.
     * @param <TT> The type of the metadata that's being fetched.
     */
    <TT extends T> Optional<TT> fetchMetadata(String key);
    
    /**
     * Removes a metadata from the family.
     * @param key The name of the metadata to remove.
     */
    void removeMetadata(String key);
    
    /**
     * @return A map containing all of this family's metadata.
     */
    Map<String, T> metadata();
}
