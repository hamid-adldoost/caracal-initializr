import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {InnerComponent} from './inner/inner.component';
import {JalaliPipe} from './pipes/jalali.pipe';
import {JalaliTimePipe} from './pipes/jalali-time.pipe';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {ComponentsModule} from './components/components.module';
import {RouterModule} from '@angular/router';
import {DpDatePickerModule} from 'ng2-jalali-date-picker';
import {AppRoutingModule} from './app.routing';
import {AuthService} from './http-interceptor/auth.service';
import {AefHttpInterceptor} from './http-interceptor/aef-http-interceptor';
import {DataTableModule} from 'primeng/datatable';
import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {PanelModule} from 'primeng/panel';
import {
  AutoCompleteModule, CheckboxModule, ConfirmDialogModule, DropdownModule, InputSwitchModule, KeyFilterModule,
  MessagesModule,
  OrganizationChartModule, RadioButtonModule, SelectButtonModule, SpinnerModule, TreeTableModule, PanelMenuModule, MenuItem,
  FileUploadModule,
} from 'primeng/primeng';
import {GrowlModule} from 'primeng/growl';
import {MessageService} from 'primeng/components/common/messageservice';
import {HttpStatusService} from './http-interceptor/http-status.service';
import {DashboardService} from './dashboard/dashboard.service';
import {ConfirmationService, TreeNode} from 'primeng/api';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MessageModule} from 'primeng/message';
import {InplaceModule} from 'primeng/inplace';
import {DialogModule} from 'primeng/dialog';
import {ChartModule} from 'primeng/chart';
import {LoginComponent} from './login/login.component';
import {ToastModule} from 'primeng/toast';
import {LoginService} from './login/login.service';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    InnerComponent,
    JalaliPipe,
    JalaliTimePipe,
    LoginComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    ComponentsModule,
    RouterModule,
    AppRoutingModule,
    DataTableModule,
    TableModule,
    ButtonModule,
    PanelModule,
    DropdownModule,
    GrowlModule,
    KeyFilterModule,
    AutoCompleteModule,
    DpDatePickerModule,
    OrganizationChartModule,
    CheckboxModule,
    RadioButtonModule,
    SelectButtonModule,
    SpinnerModule,
    MessageModule,
    MessagesModule,
    InplaceModule,
    InputSwitchModule,
    TreeTableModule,
    DialogModule,
    ConfirmDialogModule,
    ChartModule,
    PanelMenuModule,
    PanelMenuModule,
    FileUploadModule,
    ToastModule,
    BrowserAnimationsModule,
  ],
  providers: [
    AuthService,
    MessageService,
    HttpStatusService,
    DashboardService,
    ConfirmationService,
    LoginService,
    HttpClientModule,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AefHttpInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
