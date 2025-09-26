// src/components/RedirectIfAuthenticated.tsx
import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/driveStore";

const RedirectIfAuthenticated: React.FC<{ children?: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();
  const {token} = useAuthStore()

  useEffect(() => {
    if (token) {
      // replace so user can't go "back" to landing with back button
      navigate("/home", { replace: true });
    }
    // else do nothing and allow landing page to render
  }, [navigate]);

  return <>{children}</>;
};

export default RedirectIfAuthenticated;
