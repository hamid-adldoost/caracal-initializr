package com.aef.initializr;

import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FrontGenerator {

    public String generateStructureOfProject(String projectName, String targetPath) {

        String rootPath = targetPath + "/" + projectName;
        File filePath = new File(rootPath.toString());
        filePath.mkdirs();

        generateFrontProject(targetPath, projectName);

        return rootPath;
    }

    public static void generateFrontProject(String targetPath, String projectName) {
        try {
            System.out.println("Working Directory = " +
                    System.getProperty("user.dir"));
            File file = ResourceUtils.getFile("src/main/resources/aef2ng.zip");
            String rootPath =targetPath;
            File filePath = new File(rootPath);
            filePath.mkdirs();
            extractZip(file, filePath);
            replaceText(filePath.getPath(), projectName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void replaceText(String frontProjectPath, String projectName) {

        File folder = new File(frontProjectPath);
        File[] listOfFiles = folder.listFiles();
        for(File file : listOfFiles) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String line = "", oldtext = "";
                while ((line = reader.readLine()) != null) {
                    oldtext += line + "\r\n";
                }
                reader.close();

                String replacedtext = oldtext.replaceAll("general-web", projectName);

                FileWriter writer = new FileWriter(file);
                writer.write(replacedtext);

                writer.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }


    public static void extractZip(File zipfile, File outdir) {
        try {
            ZipInputStream is = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipfile)));
            ZipEntry entry;
            while ((entry = is.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    mkDirs(outdir, name);
                } else {
                    String dir = directoryPart(name);
                    if (dir != null) {
                        mkDirs(outdir, dir);
                    }
                    extractFile(is, outdir, name);
                }
            }
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract file.
     *
     * @param inputStream
     *            the input stream
     * @param outDir
     *            the out dir
     * @param name
     *            the name
     * @throws IOException
     *             the io exception
     */
    private static void extractFile(InputStream inputStream, File outDir,
                                    String name) throws IOException {

        int BUFFER_SIZE = 4096;

        int count = -1;
        byte buffer[] = new byte[BUFFER_SIZE];
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(new File(outDir, name)), BUFFER_SIZE);
        while ((count = inputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
            out.write(buffer, 0, count);
        }
        out.close();
    }


    private static void mkDirs(File outdir, String path) {
        File d = new File(outdir, path);
        if (!d.exists()) {
            d.mkdirs();
        }
    }

    /**
     * Directory part string.
     *
     * @param name
     *            the name
     * @return the string
     */
    private static String directoryPart(String name) {
        int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }




}
