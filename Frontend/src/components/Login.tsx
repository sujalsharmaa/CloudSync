import React from "react";

const Login: React.FC = () => {
  const loginWithGoogle = () => {
    window.open("http://mylb-627534277.us-east-1.elb.amazonaws.com/api/auth/login/google", "_self");
  };

  return (
    <button
      type="button"
      aria-label="Sign in with Google"
      onClick={loginWithGoogle}
      className="inline-flex justify-center items-center px-4 py-2 border border-gray-300 rounded-md bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 shadow-sm"
    >
      {/* Google G logo (official multi-color G) */}
      <svg
        className="w-5 h-5 mr-3"
        viewBox="0 0 533.5 544.3"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-hidden="true"
      >
        <path fill="#4285F4" d="M533.5 278.4c0-18.7-1.5-37.5-4.7-55.6H272v105.2h146.9c-6.3 34-25 62.8-53.5 82v68.1h86.4c50.4-46.5 81.7-115 81.7-199.7z"/>
        <path fill="#34A853" d="M272 544.3c72.7 0 133.9-24.1 178.6-65.3l-86.4-68.1c-23.9 16-54.5 25.3-92.2 25.3-70.9 0-131-47.9-152.2-112.2H34.3v70.5C78.8 486.3 168.4 544.3 272 544.3z"/>
        <path fill="#FBBC05" d="M119.8 323.9c-10.7-31.9-10.7-66.4 0-98.3V155.1H34.3C12.2 200 0 244.8 0 289.9s12.2 89.9 34.3 134.8l85.5-100.8z"/>
        <path fill="#EA4335" d="M272 107.7c39.6 0 75.1 13.6 103.1 40.4l77.4-77.4C405.9 24.1 344.7 0 272 0 168.4 0 78.8 58 34.3 155.1l85.5 70.5C141 155.6 201.1 107.7 272 107.7z"/>
      </svg>

      <span className="text-sm font-medium text-gray-700">Sign in with Google</span>
    </button>
  );
};

export default Login;
