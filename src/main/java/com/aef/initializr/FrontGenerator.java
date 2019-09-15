package com.aef.initializr;

import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
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
            String rootPath = targetPath;
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
                "import {MessageService} from 'primeng/api';\n" +
                "import {CommonService} from '../common.service';\n" +
                "import {ConfirmationService} from 'primeng/api';\n" +
                "\n" +
                "@Component({\n" +
                "  selector: 'app-#entity',\n" +
                "  templateUrl: './#entity.component.html',\n" +
                "  styleUrls: ['./#entity.component.css']\n" +
                "})\n" +
                "export class #EntityComponent implements OnInit {\n" +
                "\n" +
                "  constructor(private #LowerCaseService: #EntityService,\n" +
                "              private messageService: MessageService,\n" +
                "              private commonService: CommonService,\n" +
                "              private confirmationService: ConfirmationService) { \n" +
                "\n" +
                "   }\n" +
                "\n" + "");

//                fields.forEach((k, v) -> {
//                    content.append("\n   " + k + " : any;");
//                });

        content.append(
                "   #LowerCase: any;\n" +
                        "\n" +
                        "\n" +
                        "  items = {data: [], count : 0};" +
                        "\n" +
                        "  ngOnInit() {\n" +
                        "    this.#LowerCase =  new Object();\n" +
                        "    this.#LowerCaseService.list(new QueryOptions(), 'search').subscribe(res => {\n" +
                        "      console.log('list call res', res);\n" +
                        "      this.items = res;\n" +
                        "    });\n" +
                        "  }\n");

        content.append("  loadItems(event: any) {\n" +
                "    if (!event) {\n" +
                "      event = {first : 0, rows : 20};\n" +
                "    }\n" +
                "    let query = new QueryOptions();\n" +
                "    query.options = [{key: 'firstIndex', value: event.first}, {key: 'pageSize', value: event.rows}];\n" +
                "    this.#LowerCaseService.list(query, 'search').subscribe(res => {\n" +
                "      this.items = res;\n" +
                "    });\n" +
                "  }\n\n");

        content.append("  save() {\n" +
                "    this.#LowerCaseService.create(this.#LowerCase, 'save').subscribe(res => {\n" +
                "      this.#LowerCase = res;\n" +
                "      this.loadItems(null);\n" +
                "      this.commonService.showSubmitMessage();\n" +
                "    });\n" +
                "  }\n" +
                "\n" +
                "  delete(id: number) {\n" +
                "    this.#LowerCaseService.delete(id, 'delete').subscribe(res => {\n" +
                "      this.commonService.showDeleteMessage();\n" +
                "      this.loadItems(null);\n" +
                "    });\n" +
                "  }\n" +
                "\n" +
                "\n" +
                "  edit(item) {\n" +
                "    this.#LowerCase = item;\n" +
                "  }\n" +
                "\n" +
                "\n" +
                "  clear() {\n" +
                "    this.#LowerCase = new Object();\n" +
                "  }\n" +
                "\n" +
                "   confirm() {\n" +
                "        this.confirmationService.confirm({\n" +
                "            message: 'آیا مطمئن هستید؟',\n" +
                "            accept: () => {\n" +
                "                // Actual logic to perform a confirmation\n" +
                "               this.delete(this.#LowerCase.id);\n" +
                "            }\n" +
                "        });\n" +
                "    }");

        content.append(
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
        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/" + GeneratorTools.camelToSnake(entityName).toLowerCase() + ".component.css"))) {
            out.print("");
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
                "        super(httpClient, '" + GeneratorTools.pascalCaseToCamelCase(entityName) + "');\n" +
                "      }\n" +
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

    public static String generateEntityHtmlView(String path, String entityName, String entityFarsiName, Map<String, String> fields, Map<String, String> farsiFieldsNames) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("<p-growl></p-growl>\n" +
                "<div class=\"main-content\">\n" +
                "\n" +
                "  <div class=\"ui-rtl\" dir=\"rtl\">\n" +
                "    <p-panel>\n" +
                "      <p-header>\n" +
                "        #FarsiName\n" +
                "      </p-header>\n" +
                "\n");
        fields.forEach((k, v) -> {
            content.append(
                    "      <div class=\"row\" style=\"direction: rtl\">\n" +
                            "        <div class=\"col-lg-4\">\n" +
                            "\n" +
                            "        </div>\n" +
                            "        <div class=\"col-lg-4\" style=\"text-align: center;\">\n");

            if (v.toLowerCase().contains("Date".toLowerCase())) {
                content.append("        <dp-date-picker \n" +
                        "                dir=\"rtl\"\n" +
                        "                [(ngModel)]=\"#LowerCase." + k + "\"\n" +
                        "                mode=\"day\"\n" +
                        "                placeholder=\"تاریخ\"\n" +
                        "                theme=\"dp-material\">\n" +
                        "          </dp-date-picker>\n");
            } else {
                content.append("          <input pInputText type=\"text\" [(ngModel)]=\"#LowerCase." + k + "\" >\n");
            }
            content.append("        </div>\n");
            content.append("        <div class=\"col-lg-4\">\n" +
                    "\n" +
                    "       " + farsiFieldsNames.get(k) + "\n" +
                    "        </div>\n" +
                    "    </div>\n");
        });
        content.append(
                "      <div class=\"row\" style=\"margin-top: 30px;\">\n" +
                        "        <div class=\"col-lg-4\">\n" +
                        "          <button type=\"button\" *ngIf=\"this.#LowerCase.id\" (click)=\"clear()\" class=\"btn btn-info\">جدید</button>\n" +
                        "        </div>\n" +
                        "        <div class=\"col-lg-4\">\n" +
                        "          <button type=\"button\" *ngIf=\"this.#LowerCase.id\" (click)=\"confirm()\" class=\"btn btn-danger\">حذف</button>\n" +
                        "        </div>\n" +
                        "        <div class=\"col-lg-4\">\n" +
                        "          <button type=\"button\" (click)=\"save()\" class=\"btn btn-info\">ذخیره</button>\n" +
                        "        </div>\n" +
                        "      </div>\n" +
                        "\n" +
                        "      <div class=\"row\" *ngIf=\"items\" style=\"margin-top: 5px;\">\n" +
                        "        <p-table [value]=\"items.data\" [responsive]=\"true\" [paginator]=\"true\" (onLazyLoad)=\"loadItems($event)\"\n" +
                        "                 [paginator]=\"true\"\n" +
                        "                 [rows]=\"20\" [totalRecords]=\"items.count\" emptymessage=\"درخواستی یافت نشد\">\n" +
                        "          <ng-template pTemplate=\"header\">\n" +
                        "            <tr>\n");
        content.append("              <th colspan=\"1\">#</th>\n");
        fields.forEach((k, v) -> {
            content.append("              <th colspan=\"2\">").append(farsiFieldsNames.get(k)).append("</th>\n");
        });
        content.append("              <th colspan=\"2\">ویرایش</th>\n");
        content.append("              <th colspan=\"2\">حذف</th>\n");
        content.append("            </tr>\n" +
                "          </ng-template>\n" +
                "          <ng-template pTemplate=\"body\" let-item let-i=\"rowIndex\">\n" +
                "            <tr>\n");
        content.append("              <td colspan=\"1\">{{i+1}}</td>\n");
        fields.forEach((k, v) -> {
            if(v.toLowerCase().contains("Date".toLowerCase())) {
                content.append("              <td colspan=\"2\">{{item." + k + " | jalalitime }} </td>\n");
            } else {
                content.append("              <td colspan=\"2\">{{item." + k + "}} </td>\n");
            }
        });
        content.append("              <td colspan=\"2\">" +
                "               <button type=\"button\" (click)=\"edit(item)\" class=\"btn btn-info\">ویرایش</button>\n" +
                "           </td>\n");

        content.append("              <td colspan=\"2\">" +
                "               <button type=\"button\" (click)=\"delete(item.id)\" class=\"btn btn-danger\">حذف</button>\n" +
                "           </td>\n");

        content.append("            </tr>\n" +
                "          </ng-template>\n" +
                "        </p-table>\n" +
                "      </div>\n" +
                "\n" +
                "\n" +
                "  </p-panel>\n" +
                "\n" +
                "  <p-confirmDialog header=\"توجه\" icon=\"pi pi-exclamation-triangle\" acceptLabel=\"بله\"\n" +
                "                       rejectLabel=\"خیر\"></p-confirmDialog>\n" +
                "  </div>\n" +
                "</div>\n");
        String result = content.toString();
        result = result.replace("#LowerCase", entityName.toLowerCase());
        result = result.replace("#entity", entityName);
        result = result.replace("#FarsiName", entityFarsiName);

        System.out.printf(result);
        path += "src\\app\\" + GeneratorTools.camelToDashedSnake(entityName).toLowerCase() + "\\";
        File file = new File(path);
        file.mkdirs();
        try (PrintStream out = new PrintStream(new FileOutputStream(path + "/" + GeneratorTools.camelToSnake(entityName).toLowerCase() + ".component.html"))) {
            out.print(result);
        }
        return result;
    }

    public static String refactorAppModule(String path, List<String> entityNames) throws IOException {
        path += "src\\app\\app.module.ts";
        String content = new String ( Files.readAllBytes( Paths.get(path) ) );

        String oldDecleration = "LoginComponent,";

        StringBuilder refactoredDecleration = new StringBuilder(
                "LoginComponent,\n");
                entityNames.forEach(e -> {
                    refactoredDecleration.append("    " + e + "Component,\n");
                });

        content = content.replace(oldDecleration, refactoredDecleration);


        StringBuilder oldImport = new StringBuilder("import {LoginService} from './login/login.service';\n");
        StringBuilder refactoredImport = new StringBuilder("import {LoginService} from './login/login.service';\n");
        entityNames.forEach(e -> {
            refactoredImport.append("import {").append(e).append("Service} from './").append(GeneratorTools.camelToDashedSnake(e)).append("/").append(GeneratorTools.camelToSnake(e)).append(".service';\n");
            refactoredImport.append("import {").append(e).append("Component} from './").append(GeneratorTools.camelToDashedSnake(e)).append("/").append(GeneratorTools.camelToSnake(e)).append(".component';\n");
        });
        content = content.replace(oldImport, refactoredImport);

        File file = new File(path);
        file.delete();
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {
            out.print(content);
        }
        return content;
    }


    public static void replaceText(String frontProjectPath, String projectName) {

        File folder = new File(frontProjectPath);
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
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
     * @param inputStream the input stream
     * @param outDir      the out dir
     * @param name        the name
     * @throws IOException the io exception
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
     * @param name the name
     * @return the string
     */
    private static String directoryPart(String name) {
        int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }


}
