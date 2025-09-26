import React, { useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { motion, Variants, useReducedMotion } from "framer-motion";
import { Button } from "@/components/ui/button";

const containerVariants: Variants = {
  hidden: { opacity: 0, y: 40 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      staggerChildren: 0.14,
      when: "beforeChildren",
    },
  },
  exit: { opacity: 0, y: 20, transition: { duration: 0.35 } },
};

const itemVariants: Variants = {
  hidden: { opacity: 0, y: 18 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: "easeOut" } },
};

const NotFound: React.FC = () => {
  const location = useLocation();
  const shouldReduceMotion = useReducedMotion();

  useEffect(() => {
    // Helpful debug log when someone hits an invalid route
    console.error(
      "404 Error: attempted to access non-existent route:",
      location.pathname
    );
  }, [location.pathname]);

  // Early-return static markup for users preferring reduced motion
  if (shouldReduceMotion) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
        <div className="w-full max-w-2xl rounded-3xl border border-gray-200 bg-card p-8 text-center shadow-lg sm:p-12">
          <div className="mb-6">
            <div className="mx-auto w-40 h-40 rounded-full bg-gradient-to-br from-indigo-600 to-sky-500 flex items-center justify-center text-3xl font-extrabold text-white">
              404
            </div>
          </div>

          <h1 className="text-3xl font-extrabold text-foreground">Whoops! Lost in the cloud.</h1>
          <p className="mt-4 text-base text-muted-foreground">
            The page you are looking for might have been moved, deleted, or never existed.
          </p>

          <div className="mt-8">
            <Link to="/" aria-label="Return to home">
              <Button className="px-8 py-4 text-lg font-bold rounded-full shadow-lg hover:shadow-xl transition-all duration-150">
                Return to Home
              </Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center overflow-hidden bg-background px-4 py-12">
      <motion.div
        className="w-full max-w-2xl rounded-3xl border border-gray-200 bg-card p-8 text-center shadow-lg sm:p-12"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        exit="exit"
        role="main"
        aria-labelledby="notfound-title"
      >
        <motion.div variants={itemVariants} className="mb-8">
          <div className="relative mx-auto w-full max-w-xs">
            {/* Animated SVG illustration */}
            <motion.svg
              width="200"
              height="200"
              viewBox="0 0 200 200"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
              className="mx-auto h-auto w-full"
            >
              {/* Outer blob stroke */}
              <motion.path
                d="M100 0 C150 0, 200 50, 200 100 S150 200, 100 200 S0 150, 0 100 S50 0, 100 0z"
                initial={{ pathLength: 0 }}
                animate={{ pathLength: 1 }}
                transition={{ duration: 1.6, ease: "easeInOut" }}
                fill="none"
                stroke="rgba(99,102,241,0.7)"
                strokeWidth={4}
                strokeLinecap="round"
              />

              {/* Inner circle */}
              <motion.circle
                cx="100"
                cy="100"
                r="40"
                initial={{ r: 0, opacity: 0 }}
                animate={{ r: 40, opacity: 1 }}
                transition={{ duration: 0.9, delay: 0.9, ease: "easeOut" }}
                fill="rgba(99,102,241,0.9)"
              />


              {/* 404 text */}
              <motion.text
                x="100"
                y="110"
                textAnchor="middle"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.5, delay: 1.9 }}
                style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, monospace", fontSize: 36, fontWeight: 800, fill: "#fff" }}
              >
                404
              </motion.text>
            </motion.svg>
          </div>
        </motion.div>

        <motion.h1
          id="notfound-title"
          className="my-4 text-4xl font-extrabold text-foreground sm:text-6xl"
          variants={itemVariants}
        >
          Whoops! Not Found
        </motion.h1>

        <motion.p
          className="mb-6 text-base text-muted-foreground sm:text-lg"
          variants={itemVariants}
        >
          The page you are looking for might have been moved, deleted, or never existed. Don't worry — we'll get you back on track.
        </motion.p>

        <motion.div variants={itemVariants}>
          <Link to="/" aria-label="Return to Home">
            <Button
              className="px-8 py-4 text-lg font-bold rounded-full shadow-lg hover:shadow-xl focus:outline-none focus-visible:ring-4 focus-visible:ring-indigo-400 transition-all duration-300"
              size="lg"
            >
              Return to Home
            </Button>
          </Link>
        </motion.div>
      </motion.div>
    </div>
  );
};

export default NotFound;
