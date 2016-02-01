package org.novapeng.jpax;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 * Created by pengchangguo on 15/10/23.
 */
public class ClassUtil {

    /**
     * 从包package中获取所有的Class
     *
     * @param pack package name
     * @return class collection
     */
    public static Set<Class> getClasses(String pack) {
        Set<Class> classes = new LinkedHashSet<Class>();
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(
                    packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath,
                            true, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection())
                                .getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                if (idx != -1) {
                                    packageName = name.substring(0, idx)
                                            .replace('/', '.');
                                }
                                if (name.endsWith(".class")
                                        && !entry.isDirectory()) {
                                    String className = name.substring(
                                            packageName.length() + 1, name
                                                    .length() - 6);
                                    FileClassLoader fileClassLoader = new FileClassLoader(ClassUtil.class.getClassLoader());
                                    InputStream inputStream = jar.getInputStream(entry);
                                    byte[] classBytes = new byte[inputStream.available()];
                                    //noinspection ResultOfMethodCallIgnored
                                    inputStream.read(classBytes);
                                    Class clazz = fileClassLoader.findClass(className, classBytes);
                                    if (clazz != null) {
                                        classes.add(clazz);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new JPAException("to enhance jpa class error! ", e);
                    }
                }
            }
        } catch (IOException e) {
            throw new JPAException("to enhance jpa class error! ", e);
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName 包名称
     * @param packagePath 包路径
     * @param recursive 是否循环迭代
     * @param classes 结果集
     */
    public static void findAndAddClassesInPackageByFile(String packageName,
                                                        String packagePath, final boolean recursive, Set<Class> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "."
                                + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                String className = file.getName().substring(0,
                        file.getName().length() - 6);
                FileClassLoader fileClassLoader = new FileClassLoader(ClassUtil.class.getClassLoader());
                Class clazz = fileClassLoader.findClass(className, file.getAbsolutePath());
                if (clazz != null) {
                    classes.add(clazz);
                }
            }
        }
    }

    public static class FileClassLoader extends ClassLoader {

        public FileClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class findClass(@SuppressWarnings("UnusedParameters") String name, String filePath) {
            byte[] data = loadClassData(filePath);
            try {
                return defineClass(null, data, 0, data.length);
            } catch (IllegalAccessError e) {
                return null;
            }
        }

        public Class findClass(@SuppressWarnings("UnusedParameters") String name, byte[] data) {
            try {
                return defineClass(null, data, 0, data.length);
            } catch (IllegalAccessError e) {
                return null;
            }
        }

        private byte[] loadClassData(String filePath) {
            FileInputStream fis;
            byte[] data;
            try {
                fis = new FileInputStream(new File(filePath));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = fis.read()) != -1) {
                    baos.write(ch);
                }
                data = baos.toByteArray();
            } catch (IOException e) {
                throw new JPAException("enhance file " + filePath + " failded", e);
            }
            return data;
        }

    }
}
