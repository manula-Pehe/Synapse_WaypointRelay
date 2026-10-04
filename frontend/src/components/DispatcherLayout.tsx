import React, { useState } from 'react';
import SideNav from './SideNav';
import type { SideNavProps } from './SideNav';
import TopBar from './TopBar';
import NotificationsDrawer from './NotificationsDrawer';

export interface DispatcherLayoutProps {
  children: React.ReactNode;
  activeNav?: string;
  onNavChange?: (nav: string) => void;
  title?: string;
  subtitle?: string;
  activeDepot?: string;
  onDepotChange?: (depot: string) => void;
  runDate?: string;
  planStatus?: string;
  notificationCount?: number;
  user?: SideNavProps['user'];
  onSignOut?: () => void;
  onNotificationClick?: () => void;
  hideTopBar?: boolean;
}

export const DispatcherLayout: React.FC<DispatcherLayoutProps> = ({
  children,
  activeNav,
  onNavChange,
  title,
  subtitle,
  activeDepot,
  onDepotChange,
  runDate,
  planStatus,
  notificationCount = 8,
  user,
  onSignOut,
  onNotificationClick,
  hideTopBar = false,
}) => {
  const [isNotifOpen, setIsNotifOpen] = useState(false);

  const handleNotificationClick = () => {
    setIsNotifOpen(true);
    onNotificationClick?.();
  };

  return (
    <div className="flex h-screen w-full bg-white overflow-hidden font-sans text-slate-800 antialiased">
      {/* Left Sidebar Navigation */}
      <SideNav
        activeItem={activeNav}
        onItemSelect={onNavChange}
        user={user}
        onSignOut={onSignOut}
      />

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col overflow-y-auto bg-[#f8fafc]">
        {/* Top Navigation Bar */}
        {!hideTopBar && (
          <TopBar
            title={title}
            subtitle={subtitle}
            activeDepot={activeDepot}
            onDepotChange={onDepotChange}
            runDate={runDate}
            planStatus={planStatus}
            notificationCount={notificationCount}
            onNotificationClick={handleNotificationClick}
          />
        )}

        {/* Content Area Rendering Children */}
        <div className="flex-1 p-8">
          {children}
        </div>
      </main>

      {/* Notifications Drawer Overlay */}
      <NotificationsDrawer
        isOpen={isNotifOpen}
        onClose={() => setIsNotifOpen(false)}
      />
    </div>
  );
};

export default DispatcherLayout;
