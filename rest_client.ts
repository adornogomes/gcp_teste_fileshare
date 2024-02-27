Here's the equivalent code in Angular using HttpClient:

typescript
Copy code
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ExampleService {

  constructor(private http: HttpClient) { }

  makeRequestAndSaveZipFile(): Observable<any> {
    const url = 'http://localhost:8080/hello/downloadFromListObjects';

    // Create the request body
    const requestBody = {
      bucketName: 'bucket_de_pdfs',
      objectNames: ['1-s2.0-S0167739X16000029-main.pdf', 'meus_docs/']
    };

    // Set headers
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    // Make the request
    return this.http.post(url, requestBody, {
      headers: headers,
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(
      catchError(error => {
        console.error('Failed to make the request:', error);
        throw error;
      })
    );
  }

  saveZipFile(response: HttpResponse<ArrayBuffer>): void {
    if (response.status === 200) {
      // Save the zip file
      const blob = new Blob([response.body], { type: 'application/zip' });
      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.download = 'download.zip';
      anchor.href = url;
      anchor.click();
      window.URL.revokeObjectURL(url);
      console.log('Zip file saved successfully.');
    } else {
      console.error('Failed to download folder.');
    }
  }
}
In your component, you can use this service like this:

typescript
Copy code
import { Component } from '@angular/core';
import { ExampleService } from './example.service';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  constructor(private exampleService: ExampleService) { }

  downloadAndSave(): void {
    this.exampleService.makeRequestAndSaveZipFile().subscribe(
      (response: HttpResponse<ArrayBuffer>) => {
        this.exampleService.saveZipFile(response);
      },
      error => {
        console.error('Failed to make request:', error);
      }
    );
  }
}
In your component's HTML, you can call downloadAndSave() method when you want to trigger the download:

html
Copy code
<button (click)="downloadAndSave()">Download and Save</button>
Make sure to provide the ExampleService in your Angular module's providers array.
