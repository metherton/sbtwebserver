FROM openjdk:8

COPY target/universal/stage /home/root/sbtserver/stage
WORKDIR /home/root/sbtserver/stage/bin
EXPOSE 8080
RUN chmod +x server
ENTRYPOINT ["bash", "server"]

