import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ROLE_LABELS, errorMessage } from '../../core/labels';
import { Project } from '../../core/models';
import { ProjectService } from '../../core/project.service';

/** Liste des projets de l'utilisateur connecté et formulaire de création. */
@Component({
  selector: 'app-projects',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './projects.html',
})
export class Projects implements OnInit {
  private readonly projectService = inject(ProjectService);

  readonly roleLabels = ROLE_LABELS;

  readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
    startDate: ['', Validators.required],
  });

  projects: Project[] = [];
  loading = true;
  loadError = '';
  createError = '';
  submitting = false;

  ngOnInit(): void {
    this.projectService.list().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loading = false;
      },
      error: (err) => {
        this.loadError = errorMessage(err);
        this.loading = false;
      },
    });
  }

  /** Crée le projet et l'ajoute en tête de liste ; son créateur en est l'administrateur. */
  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, description, startDate } = this.form.getRawValue();
    this.createError = '';
    this.submitting = true;
    this.projectService
      .create({ name, description: description.trim() || null, startDate })
      .subscribe({
        next: (project) => {
          this.projects = [project, ...this.projects];
          this.form.reset();
          this.submitting = false;
        },
        error: (err) => {
          this.createError = errorMessage(err);
          this.submitting = false;
        },
      });
  }
}
