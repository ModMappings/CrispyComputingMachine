FROM openjdk:11-jdk-slim
ADD build/distributions/CrispyComputingMachine-boot.tar /app/
WORKDIR /app/CrispyComputingMachine-boot/
CMD bin/CrispyComputingMachine