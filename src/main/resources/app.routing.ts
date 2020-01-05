import {NgModule} from '@angular/core';
import {CommonModule, } from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {Routes, RouterModule} from '@angular/router';
import {DashboardComponent} from './dashboard/dashboard.component';
import {InnerComponent} from './inner/inner.component';
import {LoginComponent} from './login/login.component';
import {CustomerComponent} from './customer/customer.component';


const routes: Routes = [
    {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full'
    },
    {
      path: 'login',
      component: LoginComponent,
      pathMatch: 'full'
    },
    {
        path: 'inner',
        component: InnerComponent,
        children: [
          {
            path: 'dashboard',
            component: DashboardComponent
          }
        ]
    },

];

@NgModule({
    imports: [
        CommonModule,
        BrowserModule,
        RouterModule.forRoot(routes)
    ],
    exports: [
        RouterModule
    ],
})
export class AppRoutingModule {
}
