import React from "react";
import {
  Navigate,
  Route,
  RouterProvider,
  createBrowserRouter,
  createRoutesFromElements,
} from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import PrivateRoute from "./api/PrivateRoute";
import CreateHabitPage from "./pages/CreateHabitPage";
import EditingHabitPage from "./pages/EditingHabitPage";
import HabitForCreatorPage from "./pages/HabitForCreatorPage";
import HabitForSubscriberPage from "./pages/HabitForSubscriberPage";
import LoginPage from "./pages/LoginPage";
import MyHabitsPage from "./pages/MyHabitsPage";
import MySubscriptionsPage from "./pages/MySubscriptionsPage";
import RegisterPage from "./pages/RegisterPage";

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
              <HabitForCreatorPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/subscribed-habits/:id"
          element={
            <PrivateRoute>
              <HabitForSubscriberPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/habits/:id/edit"
          element={
            <PrivateRoute>
              <EditingHabitPage />
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
