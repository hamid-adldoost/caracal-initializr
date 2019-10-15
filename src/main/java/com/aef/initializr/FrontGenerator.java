package com.aef.initializr;

import com.aef.initializr.types.DropDownType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
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

    public static String generateProxyConf(String path, String contextPath, String backendPort) throws FileNotFoundException {

        path += "proxy.conf.json";
        File file = new File(path);
        String content = "{\n" +
                "  \"#contextPath\": {\n" +
                "    \"target\": \"http://localhost:#backendPort/\",\n" +
                "    \"secure\": false,\n" +
                "    \"logLevel\" : \"debug\"\n" +
                "  }\n" +
                "\n" +
                "}";
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {

            content = content.replace("#contextPath", contextPath).replace("#backendPort", backendPort);
            out.print(content);
        }
        return content;
    }

    public static String generateEnvironment(String path, String contextPath) throws FileNotFoundException {

        path += "src/environments/environment.ts";
        File file = new File(path);
        String content = "export const environment = {\n" +
                "  production: false,\n" +
                "  baseServiceUrl: '#contextPath',\n" +
                "};";
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {

            content = content.replace("#contextPath", contextPath);
            out.print(content);
        }
        return content;
    }

    public static String generateProductionEnvironment(String path, String contextPath) throws FileNotFoundException {

        path += "src/environments/environment.prod.ts";
        File file = new File(path);
        String content = "export const environment = {\n" +
                "  production: true,\n" +
                "  baseServiceUrl: '#contextPath',\n" +
                "};";
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {

            content = content.replace("#contextPath", contextPath);
            out.print(content);
        }
        return content;
    }

    public static String generateEntityComponent(List<String> entitiesList, String path, String entityName, String entityFarsiName, Map<String, String> fields) throws FileNotFoundException {

        StringBuilder content = new StringBuilder("import { Component, OnInit } from '@angular/core';\n" +
                "import {#EntityService} from './#entity.service';\n" +
                "import {QueryOptions} from '../general/query-options';\n" +
                "import {MessageService} from 'primeng/api';\n" +
                "import {CommonService} from '../common.service';\n" +
                "import * as moment from 'jalali-moment';\n");

        fields.forEach((k, v) -> {
            if (entitiesList.contains(v)) {
                content.append("import {").append(v).append("Service} from '../").append(v.toLowerCase()).append("/").append(v.toLowerCase()).append(".service'; \n");
            }
        });
        content.append("import {ConfirmationService} from 'primeng/api';\n" +
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
                "              private commonService: CommonService,\n");
        fields.forEach((k, v) -> {
            if (entitiesList.contains(v)) {
                content.append("                private " + v.toLowerCase() + "Service: " + v + "Service,\n");
            }
        });
        content.append("              private confirmationService: ConfirmationService) { \n" +
                "\n" +
                "   }\n" +
                "\n" + "");

        fields.forEach((k, v) -> {
            if (v.toLowerCase().contains("DropDown".toLowerCase())) {
                ObjectMapper objectMapper = new ObjectMapper();
                int start = v.indexOf("[");
                int end = v.indexOf("]");
                String json = v.substring(start, end + 1);
                try {
                    DropDownType[] list = objectMapper.readValue(json, DropDownType[].class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                content.append("   " + k + "options = " + json.replace("\"", "'") + "\n");
            }
        });

        content.append(
                "   #LowerCase: any;\n" +
                        "\n" +
                        "\n" +
                        "  items = {data: [], count : 0};\n");

        fields.forEach((k, v) -> {
            if (entitiesList.contains(v)) {
                content.append("  " + k + "List = [];\n");
            }
        });

        content.append("\n" +
                "  ngOnInit() {\n");
        fields.forEach((k, v) -> {
            if (v.contains("DropDown")) {
                content.append("    this." + k + "options = this.commonService.preparePureListToDropdown(this." + k + "options);\n");
            }
        });
        content.append("    this.#LowerCase =  new Object();\n" +
                "    this.#LowerCaseService.list(new QueryOptions(), 'search').subscribe(res => {\n" +
                "      console.log('list call res', res);\n" +
                "      this.items = res;\n");
        content.append("    });\n");
        fields.forEach((k, v) -> {
            if (entitiesList.contains(v)) {
                content.append("        this.fetch" + v + "List();\n");
            }
        });
        content.append("  }\n");

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
        fields.forEach((k, v) -> {
            if (entitiesList.contains(v)) {
                content.append("  fetch" + v + "List() {\n" +
                        "    let event = {first : 0, rows : 20};\n" +
                        "    let query = new QueryOptions();\n" +
                        "    query.options = [{key: 'firstIndex', value: event.first}, {key: 'pageSize', value: event.rows}];\n" +
                        "    this." + v.toLowerCase() + "Service.list(query, 'search').subscribe(res => {\n" +
                        "      this." + k + "List = res.data;\n" +
                        "    });\n" +
                        "  }\n\n");
            }
        });

        content.append("  save() {\n");
        fields.forEach((k, v) -> {
            if (v.contains("DropDown")) {
                content.append("    if(this.#LowerCase." + k + ") { \n");
                content.append("        this.#LowerCase." + k + " = this.#LowerCase." + k + ".value;\n");
                content.append("    }\n");
            }
        });
        content.append("    this.#LowerCaseService.create(this.#LowerCase, 'save').subscribe(res => {\n" +
                "      this.#LowerCase = res;\n" +
                "      this.loadItems(null);\n" +
                "      this.commonService.showSubmitMessage();\n" +
                "      this.#LowerCase = new Object();\n" +
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
                "    this.#LowerCase = JSON.parse(JSON.stringify(item));\n");
        fields.forEach((k, v) -> {
            if (v.contains("DropDown")) {
                content.append("    this.#LowerCase." + k + " = this." + k + "options.filter(v => v.value == this.#LowerCase." + k + ")[0];\n");
            }
        });
        content.append("    this.convertDateFields();\n" +
                "  }\n" +
                "\n" +
                "\n" +
                "  clear() {\n" +
                "    this.#LowerCase = new Object();\n" +
                "  }\n" +
                "\n" +
                "   convertDateFields() {\n");

        fields.forEach((k, v) -> {
            if (k.toLowerCase().contains("date")) {
                content.append("            this.#LowerCase." + k + " = moment(this.#LowerCase." + k + ")\n");
            }
        });
        content.append("  }\n" +
                "\n" +
                "   confirm(item) {\n" +
                "        this.confirmationService.confirm({\n" +
                "            message: 'آیا مطمئن هستید؟',\n" +
                "            accept: () => {\n" +
                "                // Actual logic to perform a confirmation\n" +
                "               this.delete(item.id);\n" +
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

    public static String generateEntityHtmlView(String path, String entityName, String entityFarsiName, Map<String, String> fields, Map<String, String> farsiFieldsNames, Map<String, String> entityLabels) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("<p-toast [style]=\"{marginTop: '30px'}\" position=\"top-center\" ></p-toast>\n" +
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
                    "      <div class=\"row\" style=\"direction: rtl\">\n");
            content.append("        <div class=\"col-lg-4\">\n" +
                    "\n" +
                    "       " + farsiFieldsNames.get(k) + "\n" +
                    "        </div>\n");
            content.append("        <div class=\"col-lg-4\" style=\"text-align: right;\">\n");


            if (v.toLowerCase().contains("Date".toLowerCase())) {
                content.append("        <dp-date-picker \n" +
                        "                dir=\"rtl\"\n" +
                        "                [(ngModel)]=\"#LowerCase." + k + "\"\n" +
                        "                mode=\"day\"\n" +
                        "                placeholder=\"تاریخ\"\n" +
                        "                theme=\"dp-material\">\n" +
                        "          </dp-date-picker>\n");
            } else if (v.toLowerCase().contains("DropDown".toLowerCase())) {

                content.append("          <p-dropdown [options]=\"" + k + "options\" dataKey=\"value\" [(ngModel)]=\"#LowerCase." + k + "\" optionLabel=\"label\" dataKey=\"value\" ></p-dropdown>\n");
            } else if (AEFGenerator.getBaseTypes().contains(v)) {
                content.append("          <input pInputText type=\"text\" [(ngModel)]=\"#LowerCase." + k + "\"");
                if (GeneratorTools.isInteger(v)) {
                    content.append(" pKeyFilter=\"int\" ");
                }
                content.append(" >\n");
            } else {
                content.append("          <p-dropdown [options]=\"" + k + "List\" [(ngModel)]=\"#LowerCase." + k + "\" optionLabel=\"" + entityLabels.get(v) + "\"  dataKey=\"value\" ></p-dropdown>\n");
            }
            content.append("        </div>\n");
            content.append(
                    "        <div class=\"col-lg-4\">\n" +
                            "\n" +
                            "        </div>\n");
            content.append("    </div>\n");

        });
        content.append(
                "      <div class=\"row\" style=\"margin-top: 30px;\">\n" +
                        "        <div class=\"col-lg-12\">\n" +
                        "          <button type=\"button\" *ngIf=\"this.#LowerCase.id\" (click)=\"clear()\" class=\"btn btn-info\">جدید</button>\n" +
                        "          <button type=\"button\" *ngIf=\"this.#LowerCase.id\" (click)=\"confirm(#LowerCase)\" class=\"btn btn-danger\">حذف</button>\n" +
                        "          <button type=\"button\" (click)=\"save()\" class=\"btn btn-info\">ذخیره</button>\n" +
                        "        </div>\n" +
                        "      </div>\n" +
                        "\n" +
                        "      <div class=\"row\" *ngIf=\"items\" style=\"margin-top: 5px;\">\n" +
                        "        <p-table [value]=\"items.data\" [responsive]=\"true\" [paginator]=\"true\" (onLazyLoad)=\"loadItems($event)\"\n" +
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
            if (v.toLowerCase().contains("Date".toLowerCase())) {
                content.append("              <td colspan=\"2\">{{item." + k + " | jalalitime }} </td>\n");
//                content.append("              <td colspan=\"2\">{{item." + k + "}} </td>\n");
            } else {
                content.append("              <td colspan=\"2\">{{item." + k + "}} </td>\n");
            }
        });
        content.append("              <td colspan=\"2\">\n" +
                "               <button type=\"button\" (click)=\"edit(item)\" class=\"btn btn-info\">ویرایش</button>\n" +
                "              </td>\n");

        content.append("              <td colspan=\"2\">\n" +
                "               <button type=\"button\" (click)=\"confirm(item)\" class=\"btn btn-danger\">حذف</button>\n" +
                "              </td>\n");

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
        String content = new String(Files.readAllBytes(Paths.get(path)));

        String oldDecleration = "LoginComponent,";

        StringBuilder refactoredDecleration = new StringBuilder(
                "LoginComponent,\n");
        entityNames.forEach(e -> {
            refactoredDecleration.append("    " + e + "Component,\n");
        });

        content = content.replace(oldDecleration, refactoredDecleration);


        StringBuilder oldImport = new StringBuilder("import {LoginService} from './login/login.service';");
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

    public static String generateRouter(String path, List<String> entities) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("import {NgModule} from '@angular/core';\n" +
                "import {CommonModule, } from '@angular/common';\n" +
                "import {BrowserModule} from '@angular/platform-browser';\n" +
                "import {Routes, RouterModule} from '@angular/router';\n" +
                "import {DashboardComponent} from './dashboard/dashboard.component';\n" +
                "import {InnerComponent} from './inner/inner.component';\n" +
                "import {LoginComponent} from './login/login.component';\n");
        entities.forEach(e -> {
            content.append("import {" + e + "Component} from './" + GeneratorTools.camelToDashedSnake(e) + "/" + GeneratorTools.camelToSnake(e) + ".component';\n");
        });

        content.append("\n" +
                "\n" +
                "const routes: Routes = [\n" +
                "    {\n" +
                "        path: '',\n" +
                "        redirectTo: 'login',\n" +
                "        pathMatch: 'full'\n" +
                "    },\n" +
                "    {\n" +
                "      path: 'login',\n" +
                "      component: LoginComponent,\n" +
                "      pathMatch: 'full'\n" +
                "    },\n" +
                "    {\n" +
                "        path: 'inner',\n" +
                "        component: InnerComponent,\n" +
                "        children: [\n" +
                "          {\n" +
                "            path: 'dashboard',\n" +
                "            component: DashboardComponent\n" +
                "          },\n");

        entities.forEach(e -> {
            content.append("          {\n" +
                    "            path: '" + GeneratorTools.camelToDashedSnake(e) + "',\n" +
                    "            component: " + e + "Component\n" +
                    "          },\n");
        });
        content.append("        ]\n" +
                "    },\n" +
                "\n" +
                "];\n" +
                "\n" +
                "@NgModule({\n" +
                "    imports: [\n" +
                "        CommonModule,\n" +
                "        BrowserModule,\n" +
                "        RouterModule.forRoot(routes)\n" +
                "    ],\n" +
                "    exports: [\n" +
                "        RouterModule\n" +
                "    ],\n" +
                "})\n" +
                "export class AppRoutingModule {\n" +
                "}\n");

        path += "src\\app\\app.routing.ts";
        File file = new File(path);
        file.delete();
        String result = content.toString();
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {
            out.print(result);
        }
        return result;
    }

    public static String generateSidebarComponent(String path, List<String> entities, LinkedHashMap<String, String> entityFarsiNames) throws FileNotFoundException {
        StringBuilder content = new StringBuilder("import {Component, OnInit} from '@angular/core';\n" +
                "import {AuthService} from '../../http-interceptor/auth.service';\n" +
                "\n" +
                "declare const $: any;\n" +
                "\n" +
                "declare interface RouteInfo {\n" +
                "  path: string;\n" +
                "  title: string;\n" +
                "  icon: string;\n" +
                "  class: string;\n" +
                "  children?: RouteInfo[];\n" +
                "  showChildren?: boolean;\n" +
                "}\n" +
                "// {path: 'dashboard', title: 'داشبورد', icon: 'dashboard', class: ''},\n" +
                "export const ROUTES: RouteInfo[] = [\n" +
                "  {path: 'dashboard', title: 'داشبورد', icon: 'dashboard', class: ''},\n");

        entities.forEach(e -> {
            content.append("  {path: '").append(GeneratorTools.camelToDashedSnake(e)).append("', title: '").append(entityFarsiNames.get(e)).append("', icon: 'dashboard', class: ''},\n");
        });

        content.append("  {path: '#', title: 'راهنما', icon: '', class: '', children: [\n" +
                "\n" +
                "    ]},\n" +
                "  {path: '#', title: 'پشتیبانی', icon: '', class: '', children: [\n" +
                "\n" +
                "    ]}\n" +
                "\n" +
                "];\n" +
                "\n" +
                "@Component({\n" +
                "  selector: 'app-sidebar',\n" +
                "  templateUrl: './sidebar.component.html',\n" +
                "  styleUrls: ['./sidebar.component.css']\n" +
                "})\n" +
                "export class SidebarComponent implements OnInit {\n" +
                "  menuItems: any[];\n" +
                "\n" +
                "  constructor(private authService: AuthService) {\n" +
                "  }\n" +
                "\n" +
                "  ngOnInit() {\n" +
                "    this.menuItems = ROUTES.filter(menuItem => menuItem);\n" +
                "\n" +
                "  }\n" +
                "\n" +
                "  isMobileMenu() {\n" +
                "    if (window.screen.width > 991) {\n" +
                "      return false;\n" +
                "    }\n" +
                "    return true;\n" +
                "  }\n" +
                "\n" +
                "  activateChild(item: RouteInfo) {\n" +
                "    item.showChildren = !item.showChildren;\n" +
                "    console.log('item.showChildren', item.showChildren);\n" +
                "  }\n" +
                "\n" +
                "  logout() {\n" +
                "    this.authService.logout();\n" +
                "  }\n" +
                "\n" +
                "  getRouterLinkActive(item: any) {\n" +
                "\n" +
                "    if (item.children) {\n" +
                "      return '';\n" +
                "    } else {\n" +
                "      return 'active';\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        path += "src\\app\\components\\sidebar\\sidebar.component.ts";
        File file = new File(path);
        file.delete();
        String result = content.toString();
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {
            out.print(result);
        }
        return result;
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
