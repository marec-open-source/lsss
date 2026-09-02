import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {HelpSet} from './api/HelpSet';
import {ConfigService} from './config.service';
import {NavItem} from './misc/NavItem';

function testConfig(): object {
   return {
      helpSets: [
         {
            id: 'lsss',
            version: '2.16.0',
            buildTime: '2026-01-01T00:00:00Z',
            name: 'LSSS',
            icon: 'images/lsss.png',
            path: 'lsss/help',
            pageIds: ['', 'Interpretation'],
            pageHrefs: ['content/Top.html', 'content/Interpretation.html'],
            pageTitles: ['LSSS', 'Interpretation'],
            aliases: {},
            toc: [
               {
                  page: 0, items: [
                     {page: 1},
                     {page: 1, anchor: 'zooming', text: 'Zooming'},
                  ]
               },
            ],
         },
         {
            id: 'korona',
            version: '1.0.0',
            buildTime: '2026-01-01T00:00:00Z',
            name: 'KORONA',
            icon: 'images/korona.png',
            path: 'korona/help',
            pageIds: ['', 'Modules'],
            pageHrefs: ['content/Top.html', 'content/Modules.html'],
            pageTitles: ['KORONA', 'Modules'],
            aliases: {},
            toc: [
               {
                  page: 0, items: [
                     {page: 1},
                  ]
               },
            ],
         },
      ]
   };
}

describe('ConfigService', () => {
   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            provideRouter([]),
         ],
      });
   });

   afterEach(() => {
      TestBed.inject(HttpTestingController).verify();
   });

   async function createService(): Promise<ConfigService> {
      const service = TestBed.inject(ConfigService);
      TestBed.inject(HttpTestingController).expectOne('api/config.json').flush(testConfig());
      await service.init;
      return service;
   }

   function lsss(service: ConfigService): HelpSet {
      return service.config.helpSets[0];
   }

   it('should derive lookup tables and defaults from the config', async () => {
      const service = await createService();
      expect(service.loading()).toBe(false);
      expect(service.errorResponse()).toBeUndefined();
      expect(document.title).toEqual('LSSS 2.16.0 Help');

      const helpSet = lsss(service);
      expect(helpSet.imgSrc).toEqual('api/file/lsss/help/images/lsss.png');

      const topItem = helpSet.toc[0];
      const pageItem = topItem.items![0];
      const anchorItem = topItem.items![1];
      expect(topItem.text).toEqual('LSSS'); // Defaulted from pageTitles.
      expect(pageItem.text).toEqual('Interpretation');
      expect(anchorItem.text).toEqual('Zooming');
      expect(pageItem.parent).toBe(topItem);

      expect(helpSet.pageIdToTocItem['']).toBe(topItem);
      expect(helpSet.pageIdToTocItem['Interpretation']).toBe(pageItem);
      expect(helpSet.hrefToTocItem['content/Interpretation.html']).toBe(pageItem);
      expect(helpSet.hrefToTocItem['content/Interpretation.html#zooming']).toBe(anchorItem);
   });

   it('should report an http error', async () => {
      const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      try {
         const service = TestBed.inject(ConfigService);
         TestBed.inject(HttpTestingController).expectOne('api/config.json')
            .flush('Not found', {status: 404, statusText: 'Not Found'});
         expect(await service.init).toBe(false);
         expect(service.loading()).toBe(false);
         expect(service.errorResponse()?.status).toEqual(404);
         expect(consoleError).toHaveBeenCalled();
      } finally {
         consoleError.mockRestore();
      }
   });

   it('should find help sets by id', async () => {
      const service = await createService();
      expect(service.idToHelpSet('lsss')).toBe(service.config.helpSets[0]);
      expect(service.idToHelpSet('korona')).toBe(service.config.helpSets[1]);
      expect(service.idToHelpSet('unknown')).toBeUndefined();
   });

   it('should map nav items to file urls', async () => {
      const service = await createService();
      const helpSet = lsss(service);
      expect(service.navItemToUrl(new NavItem(helpSet, 'Interpretation')))
         .toEqual('api/file/lsss/help/content/Interpretation.html');
      expect(service.navItemToUrl(new NavItem(helpSet, 'Interpretation', 'zooming')))
         .toEqual('api/file/lsss/help/content/Interpretation.html#zooming');
   });

   it('should map nav items to router links', async () => {
      const service = await createService();
      const helpSet = lsss(service);
      expect(service.navItemToLink(new NavItem(helpSet, 'Interpretation')))
         .toEqual('#/page/lsss/Interpretation');
      expect(service.navItemToLink(new NavItem(helpSet, 'Interpretation', 'zooming')))
         .toEqual('#/page/lsss/Interpretation/zooming');
      service.routePrefix = '/set';
      expect(service.navItemToLink(new NavItem(helpSet, 'Interpretation')))
         .toEqual('#/set/lsss/Interpretation');
   });

   it('should map help refs to nav items', async () => {
      const service = await createService();
      const navItem = service.helpRefToNavItem('lsss/Interpretation/zooming');
      expect(navItem?.helpSet).toBe(lsss(service));
      expect(navItem?.pageId).toEqual('Interpretation');
      expect(navItem?.anchor).toEqual('zooming');
      expect(service.helpRefToNavItem('lsss/Interpretation')?.anchor).toEqual('');
      expect(service.helpRefToNavItem('unknown/Interpretation')).toBeUndefined();
      expect(service.helpRefToNavItem('lsss/Unknown')).toBeUndefined();
   });

   it('should map urls to nav items', async () => {
      const service = await createService();
      const navItem = service.urlToNavItem('api/file/lsss/help/content/Interpretation.html#zooming');
      expect(navItem?.helpSet).toBe(lsss(service));
      expect(navItem?.pageId).toEqual('Interpretation');
      expect(navItem?.anchor).toEqual('zooming');

      expect(service.urlToNavItem('api/file/lsss/help/content/Interpretation.html')?.anchor).toEqual('');

      // Anchors do not have to be toc entries, only the page does.
      expect(service.urlToNavItem('api/file/lsss/help/content/Interpretation.html#other')?.anchor).toEqual('other');

      const koronaNavItem = service.urlToNavItem('api/file/korona/help/content/Modules.html');
      expect(koronaNavItem?.helpSet).toBe(service.config.helpSets[1]);
      expect(koronaNavItem?.pageId).toEqual('Modules');

      expect(service.urlToNavItem('api/file/unknown/help/content/Top.html')).toBeUndefined();
      expect(service.urlToNavItem('api/file/lsss/help/content/Missing.html')).toBeUndefined();
   });

   it('should map nav items to toc items', async () => {
      const service = await createService();
      const helpSet = lsss(service);
      const topItem = helpSet.toc[0];
      const pageItem = topItem.items![0];
      const anchorItem = topItem.items![1];
      expect(service.navItemToTocItem(new NavItem(helpSet, 'Interpretation'))).toBe(pageItem);
      expect(service.navItemToTocItem(new NavItem(helpSet, 'Interpretation', 'zooming'))).toBe(anchorItem);
      // Falls back to the page toc item for anchors not in the toc.
      expect(service.navItemToTocItem(new NavItem(helpSet, 'Interpretation', 'other'))).toBe(pageItem);
   });
});
