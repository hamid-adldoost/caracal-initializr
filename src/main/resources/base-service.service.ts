import { Injectable } from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Resource} from './resource';
import {QueryOptions} from './query-options';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BaseServiceService {

  constructor(private httpClient: HttpClient,
              private baseUrl: string) { }


  public create(item: any, endpoint: string): Observable<any> {
    return this.httpClient
      .post<any>(`${environment.baseServiceUrl}/${this.baseUrl}/${endpoint}`, item);
  }

  public update(item: any, endpoint: string): Observable<any> {
    return this.httpClient
      .put<any>(`${environment.baseServiceUrl}/${this.baseUrl}/${endpoint}/${item.id}`, item);
  }

  read(id: number, endpoint: string): Observable<any> {
    return this.httpClient
      .get(`${environment.baseServiceUrl}/${this.baseUrl}/${endpoint}/${id}`);
  }

  list(queryOptions: QueryOptions, endpoint: string): Observable<any> {
    return this.httpClient.get(`${environment.baseServiceUrl}/${this.baseUrl}/${endpoint}?${queryOptions.getOptionsQueryString()}`);
  }

  delete(id: number, endpoint: string) {
    return this.httpClient
      .delete(`${environment.baseServiceUrl}/${this.baseUrl}/${endpoint}/${id}`);
  }

}
