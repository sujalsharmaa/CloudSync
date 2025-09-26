import { useEffect } from 'react';
import { useAuthStore } from '@/stores/driveStore';
import Login from './Login';
import Logout from './Logout';
import { useNavigate } from "react-router-dom";
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';

function UserStatus(): JSX.Element {
  // Use separate calls to avoid creating a new object on every render
    const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const fetchUser = useAuthStore((state) => state.fetchUser);
  const setToken = useAuthStore((state) => state.setToken);

  // Handle Google redirect URL to extract the token
  useEffect(() => {
    const url = new URL(window.location.href);
    const token = url.searchParams.get('token');

    if (token) {
      localStorage.setItem('auth_token', token);
      console.log("token ->", token);
      setToken(token);

      // Remove token from URL for cleanliness and security
       window.history.replaceState({}, document.title, '/');
       navigate("/home", { replace: true });
    }
  }, [setToken]);

  // Fetch the user data after the token is set or on component mount
  useEffect(() => {
    // Only call fetchUser if the token exists, preventing unnecessary API calls
    const storedToken = localStorage.getItem('auth_token');
    if (storedToken) {
      fetchUser();
    }
  }, [fetchUser]);

  return (
    <div className="p-4">
      {user ? (
        <>
<div className='flex gap-4'> 
  
           <Avatar className="w-8 h-8 border-2 border-background shadow-sm">
            <AvatarImage 
            loading='lazy'
            src={user?.picture} alt={user?.name} className='w-8 h-8 rounded-full'/>
          </Avatar>
          <Logout /></div>
        </>
      ) : (
        <Login />
      )}
    </div>
  );
}

export default UserStatus;
