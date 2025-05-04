import React from "react";

const GrayBlockLayout = ({ children }) => {
  return (
    <div className="bg-gray-100 shadow-md rounded-2xl p-6 mt-6 space-y-4">
      {children}
    </div>
  );
};

export default GrayBlockLayout;
