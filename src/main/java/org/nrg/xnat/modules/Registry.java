package org.nrg.xnat.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Registry {

    public ModuleMetadata getModule(final String moduleId) {
        if (_moduleMap.size() == 0) {
            for (final ModuleMetadata metadata : _modules) {
                _moduleMap.put(metadata.getModuleId(), metadata);
            }
        }
        return _moduleMap.get(moduleId);
    }

    @Autowired
    private List<ModuleMetadata> _modules;

    private final Map<String, ModuleMetadata> _moduleMap = new HashMap<>();
}
