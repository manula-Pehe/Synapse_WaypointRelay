import React, { useEffect, useState } from 'react';
import LiveBoard from './LiveBoard';
import MobileLiveBoard from './MobileLiveBoard';

export const LiveBoardPage: React.FC = () => {
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      return window.matchMedia('(max-width: 767px)').matches;
    }
    return false;
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const mediaQuery = window.matchMedia('(max-width: 767px)');

    const handleChange = (event: MediaQueryListEvent) => {
      setIsMobile(event.matches);
    };

    mediaQuery.addEventListener('change', handleChange);

    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, []);

  return isMobile ? <MobileLiveBoard /> : <LiveBoard />;
};

export default LiveBoardPage;
