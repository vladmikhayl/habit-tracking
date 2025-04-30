const reportsApi = {
  // Создать отчет о выполнении
  createReport: async (habitId, date, photoUrl) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/reports/create`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ habitId, date, photoUrl }),
    });

    if (!response.ok) {
      const data = await response.json();
      const errorMessage =
        data?.error || "Ошибка при создании отчета о выполнении";
      throw new Error(errorMessage);
    }
  },

  // Удалить отчет о выполнении
  deleteReport: async (reportId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/reports/${reportId}/delete`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      const data = await response.json();
      const errorMessage =
        data?.error || "Ошибка при удалении отчета о выполнении";
      throw new Error(errorMessage);
    }
  },

  // Изменить фото в отчете о выполнении
  changeReportPhoto: async (reportId, photoUrl) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/reports/${reportId}/change-photo`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ photoUrl }),
    });

    if (!response.ok) {
      const data = await response.json();
      const errorMessage = data?.error || "Ошибка при изменении фото";
      throw new Error(errorMessage);
    }
  },
};

export default reportsApi;
