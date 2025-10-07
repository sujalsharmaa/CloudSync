import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Routes, Route } from "react-router-dom";
import Index from "./pages/Index";
import NotFound from "./pages/NotFound";
import LandingPage from "./components/LandingPage";
import ProtectedRoute from "./ProtectedRoute"; // Import the new component
import RedirectIfAuthenticated from "./components/RedirectIfAuthenticated";
import { StoragePricingPlan } from "./pages/StoragePricingPlan";
import  PaymentSuccess  from "./pages/PaymentPageSuccess";
import  PaymentCancelled  from "./pages/PaymentPageFailure";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <Routes>
                <Route
          path="/"
          element={
            <RedirectIfAuthenticated>
              <LandingPage />
            </RedirectIfAuthenticated>
          }
        />
                <Route
          path="/storagePlans"
          element={
            <ProtectedRoute>
              <StoragePricingPlan/>
              </ProtectedRoute>
          }
        />
        <Route 
          path="/home" 
          element={
            <ProtectedRoute>
              <Index />
            </ProtectedRoute>

          } 
        />
                <Route 
          path="/paymentSuccess" 
          element={
            <ProtectedRoute>
              <PaymentSuccess/>
            </ProtectedRoute>

          } 
        />
                <Route 
          path="/paymentFailure" 
          element={
            <ProtectedRoute>
          <PaymentCancelled/>
            </ProtectedRoute>

          } 
        />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;