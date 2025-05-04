import React from "react";

import AuthLayout from "../layouts/AuthLayout";
import RegisterForm from "../components/RegisterForm";

const RegisterPage = () => {
  return (
    <AuthLayout>
      <RegisterForm />
    </AuthLayout>
  );
};

export default RegisterPage;
