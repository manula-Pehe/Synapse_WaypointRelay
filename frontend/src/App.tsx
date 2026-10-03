import { useState } from 'react';
import DispatcherLayout from './components/DispatcherLayout';
import OrderQueue from './components/OrderQueue';
import FleetStatus from './components/FleetStatus';
import LiveBoardPage from './components/LiveBoardPage';

export function App() {
  const [activeNav, setActiveNav] = useState('live-board');

  const pageMeta: Record<string, { title: string; subtitle: string; planStatus?: string }> = {
    orders: {
      title: 'Order queue · Thu 1 Oct run',
      subtitle: 'Orders closed Wed 4:00 PM · 85 confirmed orders',
      planStatus: 'Plan v1 · not started',
    },
    fleet: {
      title: 'Fleet · Peliyagoda · Thu 1 Oct',
      subtitle: 'Mark workshop vehicles before planning · weekly fuel shown per vehicle',
      planStatus: 'Plan v1 · not started',
    },
    'live-board': {
      title: 'Live board · Thu 1 Oct · 6:45 AM',
      subtitle: 'Exceptions first · updates arrive as drivers sync',
      planStatus: 'Plan v1 · published',
    },
  };

  const currentMeta = pageMeta[activeNav] || {
    title: 'Waypoint Relay',
    subtitle: 'Dispatch & Fleet Operations',
    planStatus: 'Plan v1 · not started',
  };

  const renderContent = () => {
    switch (activeNav) {
      case 'fleet':
        return <FleetStatus />;
      case 'live-board':
        return <LiveBoardPage />;
      case 'orders':
      default:
        return <OrderQueue />;
    }
  };

  return (
    <DispatcherLayout
      activeNav={activeNav}
      onNavChange={setActiveNav}
      title={currentMeta.title}
      subtitle={currentMeta.subtitle}
      planStatus={currentMeta.planStatus}
    >
      {renderContent()}
    </DispatcherLayout>
  );
}

export default App;

