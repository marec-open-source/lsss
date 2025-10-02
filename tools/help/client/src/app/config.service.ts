import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Router} from '@angular/router';
import {BehaviorSubject, Observable, ReplaySubject} from 'rxjs';
import {ClientConfig} from './api/ClientConfig';
import {HelpSet} from './api/HelpSet';
import {TocItem} from './api/TocItem';
import {NavItem} from './misc/NavItem';
import * as Utils from './misc/Utils';

@Injectable({
   providedIn: 'root',
})
export class ConfigService {
   private readonly http: HttpClient = inject(HttpClient);
   private readonly router: Router = inject(Router);

   loading: boolean = true;
   errorResponse?: HttpErrorResponse;

   readonly init: ReplaySubject<boolean> = new ReplaySubject<boolean>();
   config!: ClientConfig;

   currentHelpSet?: HelpSet;
   routePrefix: string = '';
   readonly navigation: BehaviorSubject<NavItem | undefined> = new BehaviorSubject<NavItem | undefined>(undefined);

   constructor() {
      this.http.get<ClientConfig>('api/config.json').subscribe({
         next: config => {
            this.config = config;
            this.config.helpSets.forEach(helpSet => {
               helpSet.imgSrc = Utils.normalizeUrl(`api/file/${helpSet.path}`, helpSet.icon);
               helpSet.pageIdToTocItem = {};
               helpSet.hrefToTocItem = {};
               this.initTocItems(helpSet, helpSet.toc);
            });
            const mainHelpSet = config.helpSets[0];
            const linkElement: HTMLLinkElement | null = document.querySelector('link[rel="icon"]');
            if (linkElement) {
               linkElement.href = mainHelpSet.imgSrc;
            }
            document.title = `${mainHelpSet.name} ${mainHelpSet.version} Help`;
            this.init.next(true);
            this.loading = false;
         },
         error: error => {
            this.loading = false;
            this.errorResponse = error;
            console.log(error);
         }
      });
   }

   private initTocItems(helpSet: HelpSet, tocItems: TocItem[], parent?: TocItem): void {
      for (const tocItem of tocItems) {
         tocItem.parent = parent;
         const pageIndex = tocItem.page;
         const pageId = helpSet.pageIds[pageIndex];
         const anchor = tocItem.anchor ?? '';
         if (!tocItem.text) {
            tocItem.text = helpSet.pageTitles[pageIndex];
         }
         tocItem.navItem = new NavItem(helpSet, pageId, anchor);
         if (!anchor) {
            helpSet.pageIdToTocItem[pageId] = tocItem;
         }
         const pageHref = helpSet.pageHrefs[pageIndex];
         const href = anchor ? `${pageHref}#${anchor}` : pageHref;
         helpSet.hrefToTocItem[href] = tocItem;
         if (tocItem.items) {
            this.initTocItems(helpSet, tocItem.items, tocItem);
         }
      }
   }

   idToHelpSet(helpSetId: string): HelpSet | undefined {
      return this.config.helpSets.find(helpSet => helpSet.id === helpSetId);
   }

   httpGetText(url: string): Observable<string> {
      return this.http.get(url, {responseType: 'text'});
   }

   navItemToUrl(navItem: NavItem): string {
      const href = this.navItemToHref(navItem);
      return `api/file/${navItem.helpSet.path}/${href}`;
   }

   private navItemToHref(navItem: NavItem): string {
      const pageIndex = navItem.helpSet.pageIdToTocItem[navItem.pageId].page;
      const pageHref = navItem.helpSet.pageHrefs[pageIndex];
      return navItem.anchor ? `${pageHref}#${navItem.anchor}` : pageHref;
   }

   navItemToLink(navItem: NavItem): string {
      const link = `#${this.routePrefix}/${encodeURIComponent(navItem.helpSet.id)}/${encodeURIComponent(navItem.pageId)}`;
      const anchor = encodeURIComponent(navItem.anchor);
      return anchor ? `${link}/${anchor}` : link;
   }

   helpRefToNavItem(helpRef: string): NavItem | undefined {
      const parts = helpRef.split('/');
      const helpSet = this.idToHelpSet(parts[0]);
      if (!helpSet) {
         return undefined;
      }
      const pageId = parts[1] ?? '';
      if (!helpSet.pageIdToTocItem[pageId]) {
         return undefined;
      }
      const anchor = parts[2] ?? '';
      return new NavItem(helpSet, pageId, anchor);
   }

   urlToNavItem(url: string): NavItem | undefined {
      const i = url.lastIndexOf('#');
      const pageUrl = i < 0 ? url : url.substring(0, i);
      const pagePath = pageUrl.substring('api/file/'.length);
      const helpSet = this.config.helpSets.find(hs => pagePath.startsWith(hs.path));
      if (!helpSet) {
         return undefined;
      }
      const pageHref = pagePath.substring(helpSet.path.length + 1);
      const pageTocItem = helpSet.hrefToTocItem[pageHref];
      if (!pageTocItem) {
         return undefined;
      }
      const anchor = i < 0 ? '' : url.substring(i + 1);
      const pageNavItem = pageTocItem.navItem;
      return new NavItem(helpSet, pageNavItem.pageId, anchor);
   }

   navItemToTocItem(navItem: NavItem): TocItem {
      const href = this.navItemToHref(navItem);
      const tocItem = navItem.helpSet.hrefToTocItem[href];
      return tocItem ?? navItem.helpSet.pageIdToTocItem[navItem.pageId];
   }

   replaceNavigation(navItem: NavItem): void {
      const commands = [this.routePrefix, navItem.helpSet.id];
      if (navItem.pageId) {
         commands.push(navItem.pageId);
      }
      if (navItem.anchor) {
         commands.push(navItem.anchor);
      }
      this.router.navigate(commands, {replaceUrl: true});
   }

   replaceNavigationToTop(helpSet: HelpSet): void {
      if (this.routePrefix === '/set') {
         this.replaceNavigation(new NavItem(helpSet));
      } else {
         this.replaceNavigation(helpSet.toc[0].navItem);
      }
   }

   navigateToPage(): void {
      this.router.navigate(['/page']);
   }

   navigateToHelpSet(helpSet: HelpSet): void {
      this.router.navigate(['/set', helpSet.id]);
   }

   navigateToAll(): void {
      this.router.navigate(['/all']);
   }
}
