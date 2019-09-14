package com.aef.initializr;

import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.Map;
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

    public static String generateEntityComponent(String path, String entityName, String entityFarsiName, Map<String, String> fields) throws FileNotFoundException {

        StringBuilder content = new StringBuilder("import { Component, OnInit } from '@angular/core';\n" +
                "import {#EntityService} from './#entity.service';\n" +
                "import {QueryOptions} from '../general/query-options';\n" +
                "\n" +
                "@Component({\n" +
                "  selector: 'app-#entity',\n" +
                "  templateUrl: './#entity.component.html',\n" +
                "  styleUrls: ['./#entity.component.css']\n" +
                "})\n" +
                "export class #EntityComponent implements OnInit {\n" +
                "\n" +
                "  constructor(private #LowerCaseService: #EntityService) { }\n" +
                "\n" + "");

//                fields.forEach((k, v) -> {
//                    content.append("\n   " + k + " : any;");
//                });

        content.append("\n" +
                "  #LowerCase : any;\n");

                content.append(
                "\n  items = [];\n" +
                "\n" +
                "  ngOnInit() {\n" +
                "    this.#LowerCase = {};\n" +
                "    this.#LowerCaseService.list(new QueryOptions(), 'search').subscribe(res => {\n" +
                "      console.log('list call res', res);\n" +
                "      this.items = res;\n" +
                "    });\n" +
                "  }\n" +
                "\n" +
                "}\n");

        content.append("\n\n");

        String componentFileName = GeneratorTools.camelToSnake(entityName);

        String result = content.toString();
        result = result.replaceAll("#LowerCase", entityName.toLowerCase())
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", GeneratorTools.camelToSnake(entityName));

        System.out.printf(result);
        path += "src\\app\\" + GeneratorTools.camelToDashedSnake(entityName).toLowerCase() + "\\";
        File file = new File(path);
        file.mkdirs();
        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/" + GeneratorTools.camelToSnake(entityName).toLowerCase() + ".component.ts"))) {
            out.print(result);
        }
        return result;

    }

    public static String generateEntityService(String path, String entityName) throws FileNotFoundException {

        StringBuilder content = new StringBuilder("import { Injectable } from '@angular/core';\n" +
                "import {BaseServiceService} from '../general/base-service.service';\n" +
                "import {HttpClient} from '@angular/common/http';\n" +
                "\n" +
                "@Injectable({\n" +
                "  providedIn: 'root'\n" +
                "})\n" +
                "export class " + entityName + "Service extends BaseServiceService {\n" +
                "\n" +
                "      constructor(httpClient: HttpClient) {\n" +
                "\n" +
                "        super(httpClient, '" + entityName + "');\n" +
                "      }\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "}");
        String result = content.toString();
        result = result.replaceAll("#LowerCase", entityName.toLowerCase())
                .replaceAll("#Entity", entityName)
                .replaceAll("#entity", GeneratorTools.camelToSnake(entityName));
        System.out.printf(result);
        path += "src\\app\\" + GeneratorTools.camelToDashedSnake(entityName).toLowerCase() + "\\";
        File file = new File(path);
        file.mkdirs();
        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/" + GeneratorTools.camelToSnake(entityName).toLowerCase() + ".service.ts"))) {
            out.print(result);
        }
        return result;

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
