// components/Logout.jsx
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/driveStore";
import { Button } from "@/components/ui/button";

function Logout() {
  const navigate = useNavigate();
  const { setUser, setToken } = useAuthStore();

  const handleLogout = () => {
    // clear client state
    setUser(null);
    setToken(null);

    // clear persisted token if you store it
    localStorage.removeItem("accessToken");
    localStorage.removeItem("idToken");

    // navigate back to landing page (SPA routing, no full reload)
    navigate("/", { replace: true });
  };

  return (
    <Button
      onClick={handleLogout}
      className="h-8 bg-gray-800 hover:bg-gray-600 transition-all text-white"
    >
      Logout
    </Button>
  );
}

export default Logout;
