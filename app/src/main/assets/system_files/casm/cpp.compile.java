import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CppCompiler {

    public static void main(String[] args) {
            // Ang C++ source file na i-ko-compile
                    String sourceFilePath = "main.cpp";
                            
                                    // Ang output file (Kahit anong extension tulad ng .o, .out, o walang extension)
                                            String outputTarget = "main.o"; 

                                                    System.out.println("Compiling " + sourceFilePath + " to " + outputTarget + "...");

                                                            // Command: g++ -c main.cpp -o main.o
                                                                    // Ang '-c' flag ay nagsasabing i-compile lang ang file nang hindi ito ginagawang executable
                                                                            ProcessBuilder processBuilder = new ProcessBuilder("g++", "-c", sourceFilePath, "-o", outputTarget);
                                                                                    
                                                                                            // Pagsamahin ang error stream at standard output stream
                                                                                                    processBuilder.redirectErrorStream(true);

                                                                                                            try {
                                                                                                                        // Simulan ang compilation process
                                                                                                                                    Process process = processBuilder.start();

                                                                                                                                                // Basahin ang mga babala o error mula sa compiler
                                                                                                                                                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                                                                                                                                                            String line;
                                                                                                                                                                                            while ((line = reader.readLine()) != null) {
                                                                                                                                                                                                                System.out.println("[COMPILER] " + line);
                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                            }

                                                                                                                                                                                                                                                        // Hintayin matapos ang pag-compile at kunin ang exit code
                                                                                                                                                                                                                                                                    int exitCode = process.waitFor();
                                                                                                                                                                                                                                                                                
                                                                                                                                                                                                                                                                                            if (exitCode == 0) {
                                                                                                                                                                                                                                                                                                            System.out.println("Success! Compiled target created: " + outputTarget);
                                                                                                                                                                                                                                                                                                                        } else {
                                                                                                                                                                                                                                                                                                                                        System.err.println("Compilation failed with exit code: " + exitCode);
                                                                                                                                                                                                                                                                                                                                                    }

                                                                                                                                                                                                                                                                                                                                                            } catch (IOException e) {
                                                                                                                                                                                                                                                                                                                                                                        System.err.println("Error: Hindi matakbo ang compiler. Siguraduhing naka-install ang g++ sa iyong system.");
                                                                                                                                                                                                                                                                                                                                                                                    e.printStackTrace();
                                                                                                                                                                                                                                                                                                                                                                                            } catch (InterruptedException e) {
                                                                                                                                                                                                                                                                                                                                                                                                        System.err.println("Naantala ang proseso ng pag-compile.");
                                                                                                                                                                                                                                                                                                                                                                                                                    Thread.currentThread().interrupt();
                                                                                                                                                                                                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                                                                                                                                                                                                