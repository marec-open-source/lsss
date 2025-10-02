import {HelpSet} from '../api/HelpSet';

export class NavItem {
   constructor(public helpSet: HelpSet,
               public pageId: string = '',
               public anchor: string = '') {
   }
}
