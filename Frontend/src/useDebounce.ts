// hooks/useDebounce.ts
import { useEffect, useState } from 'react';

export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    // Set a timer to update the debounced value after the delay
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    // Cleanup function: this is crucial
    // It cancels the previous timer if the value or delay changes
    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
}