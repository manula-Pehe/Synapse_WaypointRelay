import { useState } from 'react';
import DispatcherLayout from './components/DispatcherLayout';
import OrderQueue from './components/OrderQueue';
import FleetStatus from './components/FleetStatus';
import DriverApp from './features/driver/DriverApp';

export function App() {
  const [activeNav, setActiveNav] = useState('orders');

  // The driver app is its own phone-width app under /driver; the dispatcher app stays at the root.
  if (window.location.pathname.startsWith('/driver')) {
    return <DriverApp />;
  }

  const pageMeta = {
    orders: {
      title: 'Order queue · Thu 1 Oct run',
      subtitle: 'Orders closed Wed 4:00 PM · 85 confirmed orders',
    },
    fleet: {
      title: 'Fleet · Peliyagoda · Thu 1 Oct',
      subtitle: 'Mark workshop vehicles before planning · weekly fuel shown per vehicle',
    },
  }[activeNav] || {
    title: 'Waypoint Relay',
    subtitle: 'Dispatch & Fleet Operations',
  };

  return (
    <DispatcherLayout
      activeNav={activeNav}
      onNavChange={setActiveNav}
      title={pageMeta.title}
      subtitle={pageMeta.subtitle}
    >
      {activeNav === 'fleet' ? <FleetStatus /> : <OrderQueue />}
    </DispatcherLayout>
  );
}

export default App;