import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/driveStore";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogTrigger,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/components/ui/alert-dialog";

function Logout() {
  const navigate = useNavigate();
  const { setUser, setToken } = useAuthStore();
  const [open, setOpen] = useState(false);

  const handleConfirmLogout = () => {
    // clear client state
    setUser(null);
    setToken(null);

    // clear persisted token if you store it
    localStorage.removeItem("auth_token");
    localStorage.removeItem("auth_user");

    // navigate back to landing page
    navigate("/", { replace: true });
  };

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      {/* Logout button (triggers dialog) */}
      <AlertDialogTrigger asChild>
        <Button
          onClick={() => setOpen(true)}
          className="h-8 bg-gray-800 hover:bg-gray-600 transition-all text-white"
        >
          Logout
        </Button>
      </AlertDialogTrigger>

      {/* Confirmation popup */}
      <AlertDialogContent className="max-w-sm">
        <AlertDialogHeader>
          <AlertDialogTitle>Confirm Logout</AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to log out? You’ll need to sign in again to access your Drive.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={() => setOpen(false)}>
            Cancel
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirmLogout}
            className="bg-red-600 hover:bg-red-700 text-white"
          >
            Yes, Logout
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export default Logout;
