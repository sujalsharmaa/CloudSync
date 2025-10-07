import React, { FC, useEffect, useState } from 'react';
import { CheckCircle, Home } from "lucide-react";


// --- CORE INTERFACES AND BASE COMPONENT (REQUIRED for standalone file) ---

interface StatusPageProps {
  title: string; 
  message: string;
  icon: FC<React.SVGProps<SVGSVGElement>>;
  iconClass: string; 
  buttonColor: string;
  planName?: string;
  onNavigateHome: () => void; 
}

/**
 * Renders the common layout for payment status pages, handling the 5-second countdown.
 */
const BaseStatusPage: FC<StatusPageProps> = ({ 
  title, 
  message, 
  icon: Icon, 
  iconClass, 
  buttonColor, 
  planName,
  onNavigateHome 
}) => {
    const [countdown, setCountdown] = useState(5);

    // Handles the 5-second auto-redirect
    useEffect(() => {
        if (countdown === 0) { 
            onNavigateHome(); 
            return; 
        }
        const timer = setTimeout(() => setCountdown(c => c - 1), 1000);
        return () => clearTimeout(timer);
    }, [countdown, onNavigateHome]);

    return (
        <div className="min-h-screen bg-slate-950 text-white font-[Inter] flex items-center justify-center p-4">
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-8 sm:p-12 max-w-lg w-full text-center shadow-2xl shadow-slate-900/50">
                <div className={`mx-auto w-16 h-16 flex items-center justify-center rounded-full mb-6 ${iconClass}`}>
                    <Icon className="w-8 h-8 text-white" />
                </div>
                
                <h1 className="text-3xl font-bold text-white mb-2">
                    {title}
                </h1>
                
                <p className="text-gray-400 mb-8">
                    {message}
                    {planName && (
                        <span className="font-semibold text-yellow-400"> {planName} </span>
                    )}
                    {'.'}
                </p>

                <p className="text-sm text-gray-500 mb-6">
                    Redirecting to the home page in {countdown} seconds...
                </p>

                <button 
                    onClick={onNavigateHome} 
                    className={`inline-flex items-center justify-center rounded-lg text-sm font-semibold transition-colors h-10 px-4 py-2 w-full sm:w-auto ${buttonColor} text-slate-900 shadow-md hover:opacity-90`}
                >
                    <Home className="w-4 h-4 mr-2" />
                    Go Home Now
                </button>
            </div>
        </div>
    );
};


// --- SUCCESS PAGE COMPONENT ---
interface PaymentPageProps {
    upgradedPlan?: string; 
    onNavigateHome: () => void; 
}

export const PaymentSuccess: FC<PaymentPageProps> = ({ upgradedPlan = "PRO", onNavigateHome }) => (
  <BaseStatusPage
    title="Payment Successful!"
    message="Congratulations! Your account has been upgraded to the"
    planName={upgradedPlan}
    icon={CheckCircle}
    iconClass="bg-green-600/50 border border-green-500/80"
    buttonColor="bg-green-500"
    onNavigateHome={onNavigateHome}
  />
);


// --- APP ENTRY POINT (Simulates rendering this page) ---
export default function App() {
  const handleHomeNavigation = () => {
    console.log("[NAVIGATE]: Performing hard redirect to home page using window.location.href.");
    // Using window.location.href to force a full browser navigation/reload
    window.location.href = '/home';
  };

  // We can simulate fetching the upgraded plan name from a URL query parameter here, 
  // but for this demo, we use a hardcoded value.
  const upgradedPlan = "Team";

  return (
    <PaymentSuccess upgradedPlan={upgradedPlan} onNavigateHome={handleHomeNavigation} />
  );
}
