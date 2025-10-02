import {inject} from '@angular/core';
import {CanActivateFn, Routes} from '@angular/router';
import {ConfigService} from './config.service';
import {HelpAllComponent} from './help-all.component';
import {HelpPageComponent} from './help-page.component';
import {HelpSetComponent} from './help-set.component';
import {NavigationComponent} from './navigation.component';

const configGuard: CanActivateFn = () => inject(ConfigService).init;

export const routes: Routes = [
   {
      path: 'all', component: HelpAllComponent, canActivate: [configGuard], children: [
         {path: '', component: NavigationComponent},
         {path: ':helpSet', component: NavigationComponent},
         {path: ':helpSet/:page', component: NavigationComponent},
         {path: ':helpSet/:page/:anchor', component: NavigationComponent},
      ]
   },
   {
      path: 'page', component: HelpPageComponent, canActivate: [configGuard], children: [
         {path: '', component: NavigationComponent},
         {path: ':helpSet', component: NavigationComponent},
         {path: ':helpSet/:page', component: NavigationComponent},
         {path: ':helpSet/:page/:anchor', component: NavigationComponent},
      ]
   },
   {
      path: 'set', component: HelpSetComponent, canActivate: [configGuard], children: [
         {path: '', component: NavigationComponent},
         {path: ':helpSet', component: NavigationComponent},
         {path: ':helpSet/:page', component: NavigationComponent},
         {path: ':helpSet/:page/:anchor', component: NavigationComponent},
      ]
   },
   {
      path: '**', redirectTo: '/page'
   }
];
