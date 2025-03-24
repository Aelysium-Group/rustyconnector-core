package group.aelysium.rustyconnector.common.modules;

import java.util.*;

public class ModuleDependencyResolver {
    public static List<String> sortPlugins(Set<ModuleLoader.ModuleRegistrar> moduleRegistrars) throws IllegalArgumentException {
        Map<String, ModuleLoader.ModuleRegistrar> configs = new HashMap<>();
        moduleRegistrars.forEach(t->configs.put(t.name().toLowerCase(), t));
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();
        
        
        configs.forEach((k, v) -> {
            String key = k.toLowerCase();
            inDegree.put(key, 0);
            graph.put(key, new ArrayList<>());
        });
        
        Set<String> missingDependencies = new HashSet<>();
        configs.forEach((k, v) -> {
            String key = k.toLowerCase();
            
            v.dependencies().forEach(dep -> {
                String depKey = dep.toLowerCase();
                if (!configs.containsKey(depKey)) {
                    missingDependencies.add(key);
                    inDegree.put(key, -1);
                    return;
                }
                graph.get(depKey).add(key);
                inDegree.put(key, inDegree.get(key) + 1);
            });
            v.softDependencies().forEach(softDep -> {
                String softDepKey = softDep.toLowerCase();
                if (!configs.containsKey(softDepKey)) return;
                graph.get(softDepKey).add(key);
                inDegree.put(key, inDegree.get(key) + 1);
            });
        });
        
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() == 0)
                queue.add(entry.getKey());
        
        List<String> sortedList = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sortedList.add(current);
            
            for (String neighbor : graph.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                
                if (inDegree.get(neighbor) != 0) continue;
                
                queue.add(neighbor);
            }
        }
        
        Set<String> circularDependencies = new HashSet<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() > 0)
                circularDependencies.add(entry.getKey());
        
        if (!missingDependencies.isEmpty())
            System.out.println("The following plugins have missing dependencies and were not loaded: " + String.join(", ", missingDependencies));
        if (!circularDependencies.isEmpty())
            System.out.println("The following plugins have circular dependencies and were not loaded: " + String.join(", ", circularDependencies));
        
        return sortedList;
    }
}
