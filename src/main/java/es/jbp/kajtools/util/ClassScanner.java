package es.jbp.kajtools.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Escaneador de clases a traves de Spring Core
 *
 * @author jberjano
 */
public class ClassScanner {

    public static ClassScanner create() {
        return new ClassScanner();
    }

    public List<Class> findClasses(List<String> classNameList) {
        return classNameList
            .stream()
            .flatMap(s -> findClasses(s).stream())
            .collect(Collectors.toList());
    }

    private List<Class> findClasses(String className) {
        if (className.contains("*")) {
            return findClassesInPackage(className);
        } else {
            try {
                return Collections.singletonList(Class.forName(className));
            } catch (ClassNotFoundException ex) {
                return Collections.emptyList();
            }
        }
    }

    private List<Class> findClassesInPackage(String classFilterPath) {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        String basePackage = ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(classFilterPath));
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + basePackage + ".class";
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources(packageSearchPath);
        } catch (IOException ex) {
            return Collections.emptyList();
        }
        List<Class> candidates = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader metadataReader;
                try {
                    metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    candidates.add(Class.forName(metadataReader.getClassMetadata().getClassName()));
                } catch (Exception ex) {
                }
            }
        }
        return candidates;
    }
}
