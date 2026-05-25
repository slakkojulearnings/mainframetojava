package com.carddemo.online.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/batch")
public class BatchSchedulerController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired(required = false)
    private Job accountActivityJob;

    @PostMapping("/jobs/{jobName}/run")
    public ResponseEntity<?> runBatchJob(@PathVariable String jobName) {
        try {
            Job job = null;

            if ("account-activity".equals(jobName)) {
                job = accountActivityJob;
            }

            if (job == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Job not found: " + jobName));
            }

            JobParameters jobParameters = new JobParametersBuilder()
                .addDate("timestamp", new Date())
                .toJobParameters();

            JobExecution execution = jobLauncher.run(job, jobParameters);

            return ResponseEntity.ok(new JobResponse(
                execution.getJobInstance().getJobName(),
                execution.getStatus().toString(),
                execution.getStartTime(),
                execution.getEndTime()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to run job: " + e.getMessage()));
        }
    }

    static class JobResponse {
        public String jobName;
        public String status;
        public Object startTime;
        public Object endTime;

        JobResponse(String jobName, String status, Object startTime, Object endTime) {
            this.jobName = jobName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    static class ErrorResponse {
        public String error;

        ErrorResponse(String error) {
            this.error = error;
        }
    }
}
