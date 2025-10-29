import React, { FC, useState } from "react"; // 1. Import useState
import {
  Check,
  Zap,
  Users,
  HardDrive,
  Star,
  Package,
  Loader2, // 2. Import Loader2
} from "lucide-react";
import { useDriveStore } from "@/stores/driveStore";
import { useNavigate } from "react-router-dom";

// --- TYPE DEFINITIONS ---
// ... (interfaces remain the same)
interface Plan {
  name: "BASIC" | "PRO" | "TEAM";
  storage: string;
  price: number;
  interval: "mo";
  icon: FC<React.SVGProps<SVGSVGElement>>;
  features: string[];
  isFeatured: boolean;
  color: string;
}
interface CardProps {
  children: React.ReactNode;
  className?: string;
}
type ButtonVariant = "default" | "outline" | "premium";
interface ButtonProps {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  variant?: ButtonVariant;
  disabled?: boolean; // Add disabled prop
}
interface PricingCardProps {
  plan: Plan;
}

// --- MOCK UI COMPONENTS ---

const Card: FC<CardProps> = ({ children, className = "" }) => (
  <div
    className={`rounded-xl bg-card text-card-foreground shadow-2xl transition-all duration-300 ${className}`}
  >
    {children}
  </div>
);

const Button: FC<ButtonProps> = ({
  children,
  className = "",
  onClick = () => {},
  variant = "default",
  disabled = false, // Handle disabled prop
}) => {
  let baseClasses =
    "inline-flex items-center justify-center rounded-lg text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 h-10 px-4 py-2 w-full";
  // ... (variantClasses switch remains the same)
  let variantClasses = "";
  switch (variant) {
    case "outline":
      variantClasses =
        "border border-slate-700 bg-transparent text-white hover:bg-slate-800/80";
      break;
    case "premium":
      variantClasses =
        "bg-yellow-500 text-slate-900 shadow-lg shadow-yellow-500/30 hover:bg-yellow-400 transform hover:scale-[1.02] active:scale-[0.98] transition-all";
      break;
    case "default":
    default:
      variantClasses = "bg-blue-600 text-white shadow-md hover:bg-blue-700";
      break;
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled} // Apply the disabled prop
      className={`${baseClasses} ${variantClasses} ${className}`}
    >
      {children}
    </button>
  );
};

// 3. Define a reusable Spinner component
const Spinner = () => <Loader2 className="w-5 h-5 animate-spin" />;

// --- DATA STRUCTURE ---
const plans: Plan[] = [
  // ... (plans data remains the same)
  {
    name: "BASIC",
    storage: "100 GB",
    price: 1,
    interval: "mo",
    icon: Package,
    features: [
      "1 User Account",
      "Standard Encryption",
      "Basic File Sharing",
      "7-Day File Versioning",
    ],
    isFeatured: false,
    color: "text-blue-400",
  },
  {
    name: "PRO",
    storage: "1 TB",
    price: 5,
    interval: "mo",
    icon: Star,
    features: [
      "5 User Accounts",
      "Military-Grade Encryption",
      "Advanced Access Controls",
      "30-Day File Versioning",
      "Priority Support",
    ],
    isFeatured: true,
    color: "text-yellow-500",
  },
  {
    name: "TEAM",
    storage: "5 TB",
    price: 25,
    interval: "mo",
    icon: Users,
    features: [
      "Unlimited User Accounts",
      "Full Compliance Suite (GDPR, HIPAA)",
      "Dedicated Account Manager",
      "Unlimited File Versioning",
      "SSO/SAML Integration",
    ],
    isFeatured: false,
    color: "text-green-400",
  },
];

// --- PRICING CARD COMPONENT ---

export interface PaymentSessionResponse {
  sessionUrl: string;
}
const PricingCard: FC<PricingCardProps> = ({ plan }) => {
  const navigate = useNavigate();
  const { handlePayment } = useDriveStore();

  // 4. Add local loading state
  const [isLoading, setIsLoading] = useState(false);

  const handleBuyStorage = async () => {
    // 5. Set loading to true
    setIsLoading(true);
    try {
      const res = await handlePayment(plan.name, plan.price);

      if (res?.sessionUrl) {
        window.location.href = res.sessionUrl;
        // Page will redirect, no need to set isLoading(false)
      } else {
        console.error("Could not get payment session URL. Please try again.");
        setIsLoading(false); // 6. Set loading false on failure
      }
    } catch (error) {
      console.error("An error occurred during payment:", error);
      setIsLoading(false); // 6. Also set loading false on catch
    }
  };

  const isFeatured = plan.isFeatured;
  const cardClasses = isFeatured
    ? "bg-slate-900 border-2 border-yellow-500/50 shadow-yellow-500/20 hover:border-yellow-400/50"
    : "bg-slate-900/50 border border-slate-800 hover:border-slate-700";

  const buttonVariant: ButtonVariant = isFeatured ? "premium" : "outline";
  const PlanIcon = plan.icon;

  return (
    <Card
      className={`p-8 flex flex-col justify-between transition-all duration-500 ${cardClasses}`}
    >
      {/* ... (card content remains the same) ... */}
      <div className="flex-grow">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-xl font-bold tracking-wider uppercase text-gray-300">
            {plan.name}
          </h2>
          {isFeatured && (
            <span className="text-xs font-semibold py-1 px-3 rounded-full bg-yellow-500/20 text-yellow-300">
              POPULAR
            </span>
          )}
        </div>
        <div className="mb-8">
          <div className="flex items-end space-x-2">
            <span className="text-6xl font-extrabold text-white">
              ${plan.price}
            </span>
            <span className="text-xl font-medium text-gray-400 pb-1">
              /{plan.interval}
            </span>
          </div>
          <p className="mt-2 text-base text-gray-400 flex items-center space-x-2">
            <HardDrive className={`w-5 h-5 ${plan.color}`} />
            <span className="font-medium text-gray-300">
              {plan.storage} Storage Included
            </span>
          </p>
        </div>
        <div className="h-[1px] w-full bg-slate-700/50 my-6"></div>
        <ul className="space-y-4 mb-10">
          {plan.features.map((feature, index) => (
            <li key={index} className="flex items-start">
              <Check className="w-5 h-5 mr-3 flex-shrink-0 text-blue-400" />
              <span className="text-base text-gray-300">{feature}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* 7. Update Button with loading state */}
      <Button
        onClick={handleBuyStorage}
        variant={buttonVariant}
        disabled={isLoading} // Disable button when loading
        className={
          isFeatured
            ? "bg-yellow-500 text-slate-900 hover:bg-yellow-400"
            : "border-slate-700 text-white hover:bg-slate-800"
        }
      >
        {isLoading ? (
          <Spinner />
        ) : isFeatured ? (
          <>
            <Star className="w-4 h-4 mr-2" /> Go Pro
          </>
        ) : (
          "Buy Storage"
        )}
      </Button>
    </Card>
  );
};

/**
 * Main application component (Vercel style dark theme).
 */
export const StoragePricingPlan = () => {
  return (
    <div className="min-h-screen bg-slate-950 text-white font-[Inter] overflow-hidden">
      {/* ... (rest of the component remains the same) ... */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-32 relative z-10">
        <div className="absolute top-0 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-500/20 rounded-full blur-3xl opacity-30 pointer-events-none"></div>
        <div className="text-center mb-20 relative z-10">
          <p className="text-sm font-semibold text-blue-400 mb-3 tracking-widest uppercase">
            Pricing
          </p>
          <h1 className="text-5xl sm:text-6xl font-extrabold text-white mb-4 leading-tight">
            Plans that Scale with You
          </h1>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            From basic personal use to high-traffic enterprise teams, find the
            perfect storage solution.
          </p>
        </div>
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-3 lg:gap-8 relative z-10">
          {plans.map((plan) => (
            <PricingCard key={plan.name} plan={plan} />
          ))}
        </div>
        <div className="mt-24 text-center">
          <div className="inline-flex items-center space-x-3 bg-slate-800/80 border border-slate-700 p-4 rounded-xl text-gray-300 shadow-xl backdrop-blur-sm">
            <Zap className="w-6 h-6 text-blue-400 fill-blue-500/10" />
            <p className="font-medium">
              Need more than 5TB?{" "}
              <a
                href="#"
                className="text-blue-400 hover:text-blue-300 underline font-semibold"
              >
                Contact Sales
              </a>{" "}
              for Enterprise options.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};