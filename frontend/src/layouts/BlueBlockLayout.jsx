import React from "react";

const BlueBlockLayout = ({ children }) => {
  return (
    <div className="bg-blue-50 border border-blue-200 shadow-sm rounded-2xl p-6">
      {children}
    </div>
  );
};

export default BlueBlockLayout;
