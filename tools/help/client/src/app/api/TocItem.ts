import {NavItem} from '../misc/NavItem';

export interface TocItem {
   page: number;
   anchor?: string;
   text?: string;
   style?: string;
   items?: TocItem[];
   //
   // Derived:
   parent?: TocItem;
   navItem: NavItem;
}
