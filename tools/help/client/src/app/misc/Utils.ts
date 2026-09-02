import {HelpSet} from '../api/HelpSet';
import {ConfigService} from '../config.service';
import {HelpPage} from './HelpPage';
import {NavItem} from './NavItem';

export function normalizeUrl(dir: string, href: string): string {
   for (; ;) {
      if (href.startsWith('../')) {
         href = href.substring(3);
         dir = dir.substring(0, dir.lastIndexOf('/'));
         continue;
      }
      if (href.startsWith('./')) {
         href = href.substring(2);
         continue;
      }
      return `${dir}/${href}`;
   }
}

export function scrollToNavItem(configService: ConfigService, navItem: NavItem): void {
   const id = `nav/${navItem.helpSet.id}/${navItem.pageId}/${navItem.anchor}`;
   const scrollElement = document.getElementById(id);
   if (scrollElement) {
      scrollElement.scrollIntoView({block: 'start'});
   } else {
      configService.replaceNavigation(new NavItem(navItem.helpSet, navItem.pageId));
   }
}

export function addHelpPages(configService: ConfigService, helpContent: HTMLElement, helpSet: HelpSet, helpPages: HelpPage[]): void {
   helpPages.forEach(helpPage => {
      const body = new DOMParser().parseFromString(`<div>${helpPage.content}</div>`, 'text/html').body;
      const pageDiv = body.firstElementChild as HTMLElement;
      adaptContent(configService, pageDiv, helpSet, helpPage.id);
      helpContent.append(pageDiv);
   });
}

export function adaptContent(configService: ConfigService, element: HTMLElement, helpSet: HelpSet, pageId: string): void {
   const pageUrl = configService.navItemToUrl(new NavItem(helpSet, pageId));
   element.id = `nav/${helpSet.id}/${pageId}/`;
   const dirUrl = pageUrl.substring(0, pageUrl.lastIndexOf('/'));
   element.querySelectorAll('img').forEach(img => {
      const src = img.getAttribute('src');
      if (src) {
         img.setAttribute('src', normalizeUrl(dirUrl, src));
      }
   });
   element.querySelectorAll('image,use').forEach(e => {
      const href = e.getAttribute('href');
      if (href) {
         e.setAttribute('href', normalizeUrl(dirUrl, href));
      }
   });
   element.querySelectorAll('h1,h2,h3,h4,h5,h6').forEach(e => {
      const anchor = e.id;
      if (anchor || e.tagName.endsWith('1')) {
         e.innerHTML = `<a href="#${anchor}" style="color: inherit;">${e.innerHTML}</a>`;
      }
   });
   element.querySelectorAll('[id]').forEach(e => {
      if (e instanceof SVGElement) {
         return;
      }
      const anchor = e.id;
      e.id = `nav/${helpSet.id}/${pageId}/${anchor}`;
   });
   element.querySelectorAll('a').forEach(a => {
      const href = a.getAttribute('href');
      if (!href) {
         return;
      }
      if (/^https?:/.test(href)) {
         return;
      }
      if (href.startsWith('installed://')) {
         a.setAttribute('href', `api/file/${href.substring(12)}`);
         return;
      }
      if (href.startsWith('help://')) {
         const navItem = configService.helpRefToNavItem(href.substring(7));
         if (navItem) {
            a.setAttribute('href', configService.navItemToLink(navItem));
         } else {
            console.error(`Invalid link from ${helpSet.id}/${pageId} to ${href}`);
         }
         return;
      }
      const linkUrl = href.startsWith('#') ? pageUrl + href : normalizeUrl(dirUrl, href);
      const navItem = configService.urlToNavItem(linkUrl);
      if (navItem) {
         a.setAttribute('href', configService.navItemToLink(navItem));
      } else {
         console.error(`Invalid link from ${helpSet.id}/${pageId} to ${href}`);
      }
   });
}
