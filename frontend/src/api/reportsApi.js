const reportsApi = {
  // Загрузить файл
  uploadFile: async (selectedFile) => {
    const token = localStorage.getItem("token");

    const formData = new FormData();
    formData.append("file", selectedFile);
    try {
      const response = await fetch(`/api/reports/upload-file`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      let resultText = await response.text();
      let result;

      try {
        result = JSON.parse(resultText);
      } catch (e) {
        result = null;
      }

      if (!response.ok) {
        const errorMessage = result?.error || "Ошибка при загрузке файла";
        throw new Error(errorMessage);
      }

      return resultText;
    } catch (err) {
      const message = err instanceof Error ? err.message : "";
      if (message.includes("Maximum upload size exceeded")) {
        throw new Error("Прикреплённый файл слишком большой");
      } else if (message.includes("Можно загружать только изображения")) {
        throw new Error(
          "Можно загружать только изображения (jpg, png, jpeg, webp, gif)"
        );
      } else {
        throw new Error("Ошибка при загрузке файла");
      }
    }
  },

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
