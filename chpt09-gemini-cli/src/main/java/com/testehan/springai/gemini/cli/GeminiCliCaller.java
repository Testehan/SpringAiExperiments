package com.testehan.springai.gemini.cli;

import java.io.*;

public class GeminiCliCaller {

    public static void main(String[] args) {
        // The prompt you want to send to Gemini.
        // This can be a simple string or a complex, multi-line one.
        String prompt = "Explain the concept of Dependency Injection in Java in simple terms.";

        try {
            System.out.println("Sending prompt to Gemini CLI...");
            System.out.println("---------------------------------");
            System.out.println("PROMPT: " + prompt);
            System.out.println("---------------------------------");

            // Call the gemini cli and get the response
            String response = callGemini(prompt);

            System.out.println("RESPONSE from Gemini:");
            System.out.println("---------------------------------");
            System.out.println(response);

        } catch (IOException | InterruptedException e) {
            System.err.println("An error occurred while calling the Gemini CLI.");
            e.printStackTrace();
        }
    }

    /**
     * Executes the Gemini CLI with a given prompt and returns the output.
     *
     * @param prompt The text prompt to send to Gemini.
     * @return The response from the Gemini CLI.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    public static String callGemini(String prompt) throws IOException, InterruptedException {
        // 1. Create the ProcessBuilder
        // This command assumes 'gemini' is in your system's PATH.
        // If not, you'd need to provide the full path to the executable.
        ProcessBuilder processBuilder = new ProcessBuilder("gemini");

        // Optional: Merge the error stream with the standard output stream.
        // This is convenient for capturing both normal output and errors in one place.
        processBuilder.redirectErrorStream(true);

        // 2. Start the Process
        Process process = processBuilder.start();

        // 3. Send the Prompt (write to the process's standard input)
        // We use a try-with-resources block to ensure the writer is closed automatically.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(prompt);
            // IMPORTANT: You must close the writer. This sends the EOF (End-of-File) signal
            // to the gemini process, letting it know that the input is complete.
            // Without this, the gemini process will wait for more input forever.
        }

        // 4. Read the Response (read from the process's standard output)
        StringBuilder output = new StringBuilder();
        // Use try-with-resources for the reader as well.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        // 5. Wait for the process to complete and check the exit code
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // If the exit code is non-zero, something went wrong.
            // The captured output may contain the error message.
            throw new IOException("Gemini CLI exited with a non-zero code: " + exitCode +
                    "\nOutput:\n" + output);
        }

        return output.toString();
    }
}
