import React, { useState } from 'react';
import SideNav from './SideNav';
import type { SideNavProps } from './SideNav';
import TopBar from './TopBar';
import NotificationsDrawer from './NotificationsDrawer';
import { useQuery } from '@tanstack/react-query';
import { dispatchApi } from '../features/dispatch/core/api';

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
  clockControl?: React.ReactNode;
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
  notificationCount,
  user,
  onSignOut,
  onNotificationClick,
  hideTopBar = false,
  clockControl,
}) => {
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const notices = useQuery({ queryKey: ['dispatch-notifications'], queryFn: dispatchApi.notifications, refetchInterval: 30_000 });

  const handleNotificationClick = () => {
    setIsNotifOpen(true);
    onNotificationClick?.();
  };

  return (
    <div className="flex h-screen w-full bg-surface overflow-hidden font-sans text-ink antialiased">
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
            notificationCount={notificationCount ?? notices.data?.unreadCount ?? 0}
            onNotificationClick={handleNotificationClick}
            clockControl={clockControl}
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
