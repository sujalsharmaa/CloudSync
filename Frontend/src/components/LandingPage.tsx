import React from "react";
import { Cloud, User, CreditCard, Shield, Check, LogIn, Database, Globe, Share2, Search, Zap } from "lucide-react";

// shadcn components
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogTrigger } from "@/components/ui/dialog";
import UserStatus from "./UserStatus";

type Plan = {
  id: string;
  name: string;
  priceMonthly: number;
  storageGB: number;
  highlights: string[];
  popular?: boolean;
};

const PLANS: Plan[] = [
  { id: "basic", name: "Basic", priceMonthly: 1, storageGB: 100, highlights: ["100 GB storage", "Email support", "AI malware scan"] },
  { id: "pro", name: "Pro", priceMonthly: 5, storageGB: 1024, highlights: ["1 TB storage", "Priority support", "File versioning", "Global CDN"], popular: true },
  { id: "team", name: "Team", priceMonthly: 15, storageGB: 5120, highlights: ["5 TB storage", "Team folders & roles", "Enterprise backups"] },
];

const PriceBadge: React.FC<{ plan: Plan }> = ({ plan }) => {
  return (
    <Card className={`p-8 rounded-2xl transition-all duration-300 ${plan.popular ? "bg-gradient-to-br from-slate-800 to-slate-900 border-indigo-500 shadow-xl" : "bg-slate-800 border-slate-700"} hover:shadow-2xl hover:-translate-y-1`}>
      {plan.popular && (
        <div className="absolute top-0 right-0 -mt-3 mr-4 -translate-y-1/2 -translate-x-1/2 px-4 py-1.5 rounded-full bg-indigo-600 text-white text-xs font-semibold uppercase tracking-wider shadow-lg">Most Popular</div>
      )}
      <CardContent className="p-0 text-white">
        <div className="flex items-center justify-between mb-6">
          <div>
            <div className="text-xl font-medium text-slate-200">{plan.name}</div>
            <div className="mt-1 flex items-baseline gap-2">
              <div className="text-4xl font-extrabold">${plan.priceMonthly}</div>
              <div className="text-base text-slate-400">/ month</div>
            </div>
            <div className="text-xs mt-1 text-slate-500">Billed monthly. Cancel anytime.</div>
          </div>
          <div className="text-right">
            <div className="text-lg font-bold text-indigo-400">{plan.storageGB} GB</div>
            <div className="text-xs text-slate-400">Storage</div>
          </div>
        </div>

        <ul className="mt-6 space-y-3">
          {plan.highlights.map((h) => (
            <li key={h} className="flex items-start gap-3 text-sm text-slate-300">
              <Check className="w-4 h-4 text-indigo-400 mt-1 flex-shrink-0" />
              <span>{h}</span>
            </li>
          ))}
        </ul>

        <div className="mt-8">
          <Button className="w-full text-lg py-3 font-semibold transition-all duration-300 transform hover:scale-105">
            Choose {plan.name}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

const Navbar: React.FC = () => (
  <header className="w-full px-6 md:px-12 py-5 flex items-center justify-between bg-transparent z-30 relative">
    <div className="flex items-center gap-3">
      <div className="w-10 h-10 rounded-xl flex items-center justify-center text-white shadow-xl bg-gradient-to-br from-indigo-500 to-sky-500">
        <Cloud className="w-5 h-5" />
      </div>
      <div className="font-bold text-xl text-white tracking-wide">DriveClone</div>
    </div>

    <div className="flex items-center gap-6">
      <nav className="hidden md:flex gap-6 text-sm font-medium text-slate-300">
        <a className="hover:text-indigo-400 transition-colors">Features</a>
        <a className="hover:text-indigo-400 transition-colors">Pricing</a>
        <a className="hover:text-indigo-400 transition-colors">Docs</a>
      </nav>

      <div className="flex items-center gap-4">
        <Dialog>
          <DialogTrigger asChild>
          </DialogTrigger>
          <DialogContent className="max-w-md bg-slate-800 border-slate-700 text-white">
            <div className="p-6">
              <h3 className="text-xl font-bold mb-2">Sign in to DriveClone</h3>
              <p className="text-sm text-slate-400 mb-6">Access your files and manage your subscription.</p>
              <form className="space-y-4">
                <Input placeholder="Email" className="bg-slate-700 border-slate-600 text-white placeholder-slate-400" />
                <Input placeholder="Password" type="password" className="bg-slate-700 border-slate-600 text-white placeholder-slate-400" />
                <div className="flex justify-end">
                  <Button className="font-semibold">Sign in</Button>
                </div>
              </form>
            </div>
          </DialogContent>
        </Dialog>

        <UserStatus/>
      </div>
    </div>
  </header>
);

const Hero: React.FC = () => (
  <section className="relative max-w-7xl mx-auto px-6 md:px-12 py-24 grid grid-cols-1 md:grid-cols-2 gap-16 items-center">
    {/* Animated background element */}
    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full bg-gradient-to-r from-indigo-900/10 to-transparent rounded-full opacity-60 blur-[100px] z-0 animate-pulse-slow" />
    <style>{`
      @keyframes pulse-slow {
        0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
        50% { transform: translate(-50%, -50%) scale(1.05); opacity: 0.7; }
      }
    `}</style>
    
    <div className="relative z-10">
      <h1 className="text-5xl md:text-6xl font-extrabold leading-tight text-white drop-shadow-lg">
        Secure, AI-powered storage for your <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-sky-400">digital life</span>.
      </h1>
      <p className="mt-6 text-xl text-slate-300 max-w-xl">
        DriveClone offers robust, multi-regional cloud storage with AI-powered security scans and a global CDN for lightning-fast access.
      </p>

      <div className="mt-8 flex gap-4">
        <Button className="px-2 py-4 text-lg font-semibold ">
         <UserStatus/>
        </Button>
        <Button variant="outline" className="px-6 py-3 text-white/80 border-white/20 hover:bg-white/10 transition-colors">
          Contact sales
        </Button>
      </div>

      <div className="mt-12 grid grid-cols-1 sm:grid-cols-2 gap-6 max-w-lg">
        <div className="flex items-start gap-4">
          <div className="p-3 rounded-xl bg-white/10 shadow-lg">
            <Shield className="w-6 h-6 text-indigo-400" />
          </div>
          <div>
            <div className="font-semibold text-white">AI-powered scanning</div>
            <div className="text-sm text-slate-400 mt-1">Automatic detection of malicious files.</div>
          </div>
        </div>

        <div className="flex items-start gap-4">
          <div className="p-3 rounded-xl bg-white/10 shadow-lg">
            <Database className="w-6 h-6 text-indigo-400" />
          </div>
          <div>
            <div className="font-semibold text-white">Multiple backups</div>
            <div className="text-sm text-slate-400 mt-1">Your files are replicated across regions.</div>
          </div>
        </div>

        <div className="flex items-start gap-4">
          <div className="p-3 rounded-xl bg-white/10 shadow-lg">
            <Globe className="w-6 h-6 text-indigo-400" />
          </div>
          <div>
            <div className="font-semibold text-white">Global CDN</div>
            <div className="text-sm text-slate-400 mt-1">Fast downloads from anywhere in the world.</div>
          </div>
        </div>

        <div className="flex items-start gap-4">
          <div className="p-3 rounded-xl bg-white/10 shadow-lg">
            <Share2 className="w-6 h-6 text-indigo-400" />
          </div>
          <div>
            <div className="font-semibold text-white">Privilege sharing</div>
            <div className="text-sm text-slate-400 mt-1">Control who can access your files.</div>
          </div>
        </div>
      </div>
    </div>

    <div className="relative z-10 flex justify-center md:justify-end">
      <div className="relative w-full max-w-md transform transition-transform duration-300 hover:scale-105">
        <div className="p-8 rounded-3xl shadow-2xl bg-gradient-to-b from-slate-900/80 to-slate-800/70 border border-slate-700 backdrop-blur-md">
          <div className="flex items-center justify-between mb-6 text-slate-200">
            <div>
              <div className="text-sm font-light text-slate-400">Your storage</div>
              <div className="text-xl font-bold">Manage plans & billing</div>
            </div>
            <div className="text-sm text-indigo-400 font-semibold">Pro Plan</div>
          </div>

          <div className="mt-4 space-y-4 text-slate-200">
            <div className="flex items-center justify-between">
              <div className="text-base text-slate-300">Used</div>
              <div className="text-base font-medium">120 GB / 1 TB</div>
            </div>

            <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
              <div className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-sky-400" style={{ width: "12%" }} />
            </div>
          </div>

          <div className="mt-8 grid grid-cols-1 gap-4">
            <Button className="w-full py-3 font-semibold transition-all duration-300 hover:opacity-80">
              Upgrade plan
            </Button>
            <Button variant="ghost" className="w-full py-3 text-white/70 border border-transparent hover:bg-white/10 transition-colors">
              View billing
            </Button>
          </div>
        </div>
        {/* Decorative shadow element */}
        <div className="absolute -right-8 -bottom-8 w-52 h-52 rounded-full bg-gradient-to-br from-indigo-500 to-sky-400 opacity-30 blur-3xl transform rotate-12" />
      </div>
    </div>
  </section>
);

const Pricing: React.FC = () => (
  <section className="relative max-w-6xl mx-auto px-6 md:px-12 py-20 overflow-hidden">
    {/* Background gradient effect */}
    <div className="absolute top-0 left-1/4 w-96 h-96 bg-indigo-500/10 rounded-full blur-[150px] -z-10 animate-blob-1" />
    <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-sky-500/10 rounded-full blur-[150px] -z-10 animate-blob-2" />
    <style>{`
      @keyframes blob-1 {
        0%, 100% { transform: translate(0, 0) scale(1); }
        33% { transform: translate(30px, -50px) scale(1.1); }
        66% { transform: translate(-20px, 20px) scale(0.9); }
      }
      @keyframes blob-2 {
        0%, 100% { transform: translate(0, 0) scale(1); }
        33% { transform: translate(-30px, 50px) scale(1.1); }
        66% { transform: translate(20px, -20px) scale(0.9); }
      }
    `}</style>
    
    <div className="text-center mb-12">
      <h3 className="text-4xl font-extrabold text-white">Simple, transparent pricing</h3>
      <p className="text-xl text-slate-400 mt-4 max-w-3xl mx-auto">
        Choose the plan that fits your needs. Scale up or down as you grow, with no hidden fees or long-term contracts.
      </p>
    </div>

    <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-8">
      {PLANS.map((p) => (
        <PriceBadge key={p.id} plan={p} />
      ))}
    </div>
  </section>
);

const App: React.FC = () => (
  <div className="relative min-h-screen bg-slate-950 font-sans antialiased text-white overflow-hidden">
    <div className="fixed inset-0 z-0 opacity-50 pointer-events-none" style={{
      backgroundImage: "radial-gradient(at 0% 0%, hsl(240, 40%, 10%) 0, transparent 50%), radial-gradient(at 100% 100%, hsl(240, 40%, 10%) 0, transparent 50%)"
    }} />
    <div className="relative z-10">
      <Navbar />
      <main>
        <Hero />
        <Pricing />
      </main>
    </div>
  </div>
);

export default App;