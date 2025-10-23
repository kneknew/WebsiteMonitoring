# Website Monitoring Tool 📊
... (Keep the previous sections: Features, Screenshots, Requirements, Configuration) ...

## Branches 🌿

* **`main`:** The stable version *without* Gemini integration.
* **`gemini`:** The development branch *with* the Google Gemini analysis feature integrated. **Checkout this branch if you want to test the Gemini analysis.**

## Build and Run 🚀

**Important:** Make sure you are on the `gemini` branch if you want to use the Gemini analysis feature.

1.  **Checkout the branch:**
    ```bash
    git checkout gemini
    ```
2.  **Build:** Open a terminal in the project's root directory and run:
    ```bash
    mvn clean install
    ```
    This will compile the code and create an executable `.jar` file in the `target/` directory.

3.  **Run:**
    * **Option 1 (From Jar file):** Ensure the `config.properties` file is in the same directory as the jar file.
        ```bash
        java -jar target/website-monitoring-1.0-SNAPSHOT.jar
        ```
        *(The jar file name might be slightly different depending on your `pom.xml` configuration)*
    * **Option 2 (Via Maven):** Run directly from source code (place `config.properties` in the project root).
        ```bash
        mvn exec:java -Dexec.mainClass="websitemonitoring.Main"
        ```

## How to Use 🖱️

1.  Enter a URL in the text field (starting with `http://` or `https://`).
2.  Click the **"Thêm"** (Add) button.
3.  Repeat to add more websites.
4.  Select the check interval (in seconds) using the spinner.
5.  Click **"Start"** to begin monitoring.
6.  Status and response times will update in the list and chart. Detailed history appears in the large text area.
7.  To remove a website, select it from the list and click **"Xóa"** (Remove).
8.  **(Gemini Branch Only):** To analyze a website using Gemini, select it from the list and click **"Phân tích"** (Analyze) (requires API Key configuration).
9.  Click **"Stop"** to cease monitoring.
10. Use the **"Import"** button to load URLs from a text file. Each line in the file should contain at least one URL. The import function will attempt to extract URLs starting with `http://` or `https://`. Invalid or duplicate URLs will be skipped.
11. Use the **"Export TXT"** button to save the current check history.
12. Use **"Xóa lịch sử"** (Clear History) to clear the history area, chart data, and status indicators.
13. Use the **"Dark Mode"/"Light Mode"** button to toggle the theme.

... (Keep the previous sections: Libraries Used, License) ...
