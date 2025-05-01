import React from "react";
import {
  Route,
  createBrowserRouter,
  createRoutesFromElements,
  RouterProvider,
  Navigate,
} from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import MyHabitsPage from "./pages/MyHabitsPage";
import MySubscriptionsPage from "./pages/MySubscriptionsPage";
import CreateHabitPage from "./pages/CreateHabitPage";
import HabitPage from "./pages/HabitPage";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import PrivateRoute from "./api/PrivateRoute";

const App = () => {
  const router = createBrowserRouter(
    createRoutesFromElements(
      <>
        <Route path="/" element={<Navigate to="/my-habits" />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/my-habits"
          element={
            <PrivateRoute>
              <MyHabitsPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/my-subscriptions"
          element={
            <PrivateRoute>
              <MySubscriptionsPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/create-habit"
          element={
            <PrivateRoute>
              <CreateHabitPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/habits/:id"
          element={
            <PrivateRoute>
              <HabitPage />
            </PrivateRoute>
          }
        />
      </>
    )
  );

  return (
    <>
      <RouterProvider router={router} />
      <ToastContainer position="top-right" autoClose={4500} />
    </>
  );
};

export default App;
