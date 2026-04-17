// sidebarState.js
export function setSidebarBodyClass(isExpanded) {
  if (typeof document !== 'undefined' && document.body) {
    if (isExpanded) {
      document.body.classList.add('sidebar-expanded');
    } else {
      document.body.classList.remove('sidebar-expanded');
    }
  }
}
